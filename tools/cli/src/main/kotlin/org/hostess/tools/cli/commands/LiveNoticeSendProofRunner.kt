package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryAttachmentSelectionResult
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.AccountLabel
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.report.ProofReportStatus

internal class LiveNoticeSendProofRunner(
    private val runtime: CliRuntime,
    private val inputs: LiveProofInputs,
    private val reportPath: String,
    private val output: CliOutput,
    private val commandName: String,
) {
    private val steps = mutableListOf<LiveProofStep>()
    private val statusFields = LiveProofStep.statusFields().toMutableMap()
    private val reportInputs = inputs.toReportInputs(CommandMode.LIVE).toMutableMap()
    private var cleanupStatus = "not_applicable"
    private var terminalFailure = false

    fun run(): CommandResult {
        if (hasForbiddenTarget()) {
            val detail = "forbidden target display name"
            statusFields["noticeSendStatus"] = "blocked"
            steps += LiveProofStep("validate-inputs", "blocked", detail)
            markNotRunUntilLogout(detail, "login")
            return finish(ProofReportStatus.BLOCKED, detail)
        }
        steps += LiveProofStep.passed("validate-inputs")
        statusFields += inputs.loginComplianceStatusFields()
        if (!operatorObservationReady()) {
            val detail = "operator observation unavailable"
            statusFields["operatorReceiptStatus"] = "blocked"
            markNotRunUntilLogout(detail, "login")
            return finish(ProofReportStatus.BLOCKED, detail)
        }
        statusFields["operatorReceiptStatus"] = if (inputs.requiresOperatorObservation()) {
            inputs.operatorReceiptStatusReportValue()
        } else {
            "not_applicable"
        }
        if (!loginStartLocation()) {
            return finish(ProofReportStatus.BLOCKED, "Agni proof start location uncontrolled")
        }

        val session = login() ?: return finish(ProofReportStatus.BLOCKED, "login blocked")
        if (!avatarReadiness(session)) {
            return finishAfterLogout(session, "avatar readiness unavailable")
        }
        if (!simulatorSession(session)) {
            return finishAfterLogout(session, "simulator session unavailable")
        }
        val groups = currentGroups(session) ?: return finishAfterLogout(session, "current groups unavailable")
        val targetSet = selectTargets(groups) ?: return finishAfterLogout(session, "target selection failed")
        val items = inventoryCatalogue(session) ?: return finishAfterLogout(session, "inventory catalogue unavailable")
        val selected = selectAttachment(items) ?: return finishAfterLogout(session, "attachment selection blocked")
        val attachment = resolveAttachment(session, selected.request) ?: return finishAfterLogout(session, "attachment resolution failed")
        if (!sendNotice(session, targetSet, selected.request, attachment)) {
            return finishAfterLogout(session, "group notice failed")
        }
        if (!noticeArchive(session, targetSet)) {
            return finishAfterLogout(session, "notice archive proof failed")
        }
        statusFields["operatorReceiptStatus"] = inputs.operatorReceiptStatusReportValue()
        runCleanup()
        return finishAfterLogout(session)
    }

    private fun hasForbiddenTarget(): Boolean =
        inputs.targetDisplayNames.any { FORBIDDEN_TARGET.matches(it.trim()) }

    private fun operatorObservationReady(): Boolean =
        !inputs.requiresOperatorObservation() || inputs.operatorObservationReady

    private fun loginStartLocation(): Boolean {
        if (!inputs.requiresOperatorObservation()) {
            statusFields["loginStartLocationStatus"] = "not_applicable"
            return true
        }
        val startLocation = runtime.loginStartLocationProbe.startLocation(
            CredentialHandle(inputs.credentialHandle.orEmpty()),
        )
        if (startLocation.isNullOrBlank()) {
            val detail = "Agni proof start location unavailable"
            statusFields["credentialStatus"] = "blocked"
            statusFields["loginStartLocationStatus"] = "blocked"
            steps += LiveProofStep("login-start-location", "blocked", detail)
            markNotRunUntilLogout(detail, "login")
            return false
        }
        val normalized = startLocation.trim()
        reportInputs["loginStartLocation"] = normalized
        return if (normalized == REQUIRED_AGNI_START_LOCATION) {
            statusFields["loginStartLocationStatus"] = "passed"
            steps += LiveProofStep.passed("login-start-location", "start=$REQUIRED_AGNI_START_LOCATION")
            true
        } else {
            val detail = "Agni proof start location uncontrolled"
            statusFields["loginStartLocationStatus"] = "blocked"
            steps += LiveProofStep("login-start-location", "blocked", detail)
            markNotRunUntilLogout(detail, "login")
            false
        }
    }

    private fun login(): HostessSession? {
        val loginRequest = LoginRequest(
            accountLabel = AccountLabel(inputs.account.orEmpty()),
            credentialHandle = CredentialHandle(inputs.credentialHandle.orEmpty()),
        )
        statusFields["credentialStatus"] = "passed"
        statusFields += inputs.loginComplianceStatusFields()
        return when (val login = runtime.sessionService.login(loginRequest, inputs.loginComplianceRequest())) {
            is SessionLoginResult.Success -> {
                statusFields["loginStatus"] = "passed"
                steps += LiveProofStep.passed("login")
                login.session
            }
            is SessionLoginResult.Failure -> {
                val detail = login.failure.redactedMessage ?: "login unavailable"
                statusFields["loginStatus"] = "blocked"
                steps += LiveProofStep("login", "blocked", detail)
                markNotRunUntilLogout(detail, "avatar-readiness")
                null
            }
        }
    }

    private fun avatarReadiness(session: HostessSession): Boolean {
        return when (val result = runtime.avatarReadinessService.ensureReady(session)) {
            is AvatarReadinessResult.Success -> {
                statusFields += avatarReadinessStatusFields(result.proof)
                steps += LiveProofStep.passed("avatar-readiness")
                true
            }
            is AvatarReadinessResult.Failure -> {
                statusFields += avatarReadinessStatusFields(result.proof)
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                steps += LiveProofStep("avatar-readiness", result.proof.avatarReadinessStatus.reportValue, detail)
                markNotRunUntilLogout(detail, "current-groups")
                false
            }
        }
    }

    private fun avatarReadinessStatusFields(proof: AvatarReadinessProof): Map<String, String> = mapOf(
        "avatarReadinessStatus" to proof.avatarReadinessStatus.reportValue,
        "simulatorPresenceStatus" to proof.simulatorPresenceStatus.reportValue,
        "regionProtocolStatus" to proof.regionProtocolStatus.reportValue,
        "agentAppearanceServiceStatus" to proof.agentAppearanceServiceStatus.reportValue,
        "cofVersionStatus" to proof.cofVersionStatus.reportValue,
        "serverAppearanceStatus" to proof.serverAppearanceStatus.reportValue,
    )

    private fun simulatorSession(session: HostessSession): Boolean {
        val outcome = LiveProofSimulatorPresenceVerifier(runtime.groupDirectoryService).verify(session)
        statusFields += outcome.statusFields
        steps += if (outcome.passed) {
            LiveProofStep.passed("simulator-session", outcome.step.detail)
        } else {
            LiveProofStep(
                "simulator-session",
                outcome.step.state,
                outcome.failureReason ?: "simulator session proof_gap",
            )
        }
        if (!outcome.passed) {
            markNotRunUntilLogout(outcome.failureReason ?: "simulator session proof_gap", "current-groups")
        }
        return outcome.passed
    }

    private fun currentGroups(session: HostessSession): List<GroupMembership>? =
        when (val result = runtime.groupDirectoryService.currentGroups(session)) {
            is GroupListResult.Success -> {
                statusFields["currentGroupsStatus"] = "passed"
                steps += LiveProofStep.passed("current-groups", "groups=${result.groups.size}")
                result.groups
            }
            is GroupListResult.Failure -> {
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                statusFields["currentGroupsStatus"] = classifyStatus(detail)
                steps += LiveProofStep("current-groups", statusFields.getValue("currentGroupsStatus"), detail)
                markNotRunUntilLogout(detail, "select-targets")
                null
            }
        }

    private fun selectTargets(groups: List<GroupMembership>): GroupTargetSet? {
        return when (
            val selection = LiveProofTargetSelector(runtime.targetSelectionService)
                .select(groups, inputs.targetDisplayNames)
        ) {
            is LiveProofTargetSelection.Selected -> {
                steps += LiveProofStep.passed("select-targets", "targets=${selection.targetSet.selectedCount}")
                selection.targetSet
            }
            is LiveProofTargetSelection.Failed -> {
                terminalFailure = true
                steps += LiveProofStep.failed("select-targets", selection.detail)
                markNotRunUntilLogout(selection.detail, "inventory-catalogue")
                null
            }
        }
    }

    private fun inventoryCatalogue(session: HostessSession): List<InventoryItemDescriptor>? =
        when (
            val result = runtime.inventoryDirectoryService.listItems(
                session,
                InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK)),
            )
        ) {
            is InventoryItemListResult.Success -> {
                statusFields["inventoryCatalogueStatus"] = "passed"
                statusFields["inventoryItemCount"] = result.items.size.toString()
                steps += LiveProofStep.passed("inventory-catalogue", "items=${result.items.size}")
                result.items
            }
            is InventoryItemListResult.Failure -> {
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                statusFields["inventoryCatalogueStatus"] = classifyStatus(detail)
                steps += LiveProofStep("inventory-catalogue", statusFields.getValue("inventoryCatalogueStatus"), detail)
                markNotRunUntilLogout(detail, "select-attachment")
                null
            }
        }

    private fun selectAttachment(items: List<InventoryItemDescriptor>): InventoryAttachmentSelectionResult.Selected? =
        when (
            val selection = runtime.inventorySelectionService.selectExistingAttachmentByDisplayName(
                items = items,
                displayName = inputs.existingAttachmentName.orEmpty(),
                kind = InventoryItemKind.LANDMARK,
                requireCopyable = true,
            )
        ) {
            is InventoryAttachmentSelectionResult.Selected -> {
                statusFields["attachmentSelectionStatus"] = "passed"
                steps += LiveProofStep.passed("select-attachment")
                selection
            }
            is InventoryAttachmentSelectionResult.NoSuchItem -> attachmentSelectionBlocked("attachment display name unavailable")
            is InventoryAttachmentSelectionResult.WrongKind -> attachmentSelectionBlocked("attachment display name has wrong kind")
            is InventoryAttachmentSelectionResult.AmbiguousDisplayName -> attachmentSelectionBlocked("attachment display name ambiguous")
            is InventoryAttachmentSelectionResult.NoCopy -> attachmentSelectionBlocked("attachment is not copyable")
            is InventoryAttachmentSelectionResult.UnknownCopyability -> attachmentSelectionBlocked("attachment copyability unknown")
        }

    private fun attachmentSelectionBlocked(detail: String): InventoryAttachmentSelectionResult.Selected? {
        statusFields["attachmentSelectionStatus"] = "blocked"
        steps += LiveProofStep("select-attachment", "blocked", detail)
        markNotRunUntilLogout(detail, "resolve-attachment")
        return null
    }

    private fun resolveAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.ExistingInventoryAttachment,
    ): AttachmentRef? =
        when (val result = runtime.attachmentService.resolveAttachment(session, request)) {
            is AttachmentResolutionResult.Resolved -> {
                statusFields["attachmentResolutionStatus"] = "passed"
                steps += LiveProofStep.passed("resolve-attachment")
                result.attachment
            }
            is AttachmentResolutionResult.Failed -> {
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                statusFields["attachmentResolutionStatus"] = classifyStatus(detail)
                steps += LiveProofStep("resolve-attachment", statusFields.getValue("attachmentResolutionStatus"), detail)
                markNotRunUntilLogout(detail, "group-notice")
                null
            }
        }

    private fun sendNotice(
        session: HostessSession,
        targetSet: GroupTargetSet,
        request: org.hostess.core.domain.ExistingInventoryAttachment,
        attachment: AttachmentRef,
    ): Boolean {
        val draft = NoticeDraft(
            subject = inputs.subject.orEmpty(),
            message = inputs.body.orEmpty(),
            targetSet = targetSet,
            attachments = listOf(request),
        )
        return when (
            val dispatch = runtime.noticeDispatchService.dispatch(
                session = session,
                draft = draft,
                attachment = attachment,
            )
        ) {
            is NoticeDispatchResult.Rejected -> noticeFailed("notice draft rejected")
            is NoticeDispatchResult.Sent -> {
                val failed = dispatch.result.statuses.firstOrNull { it.state != GroupSendState.SENT }
                if (failed == null) {
                    statusFields["noticeSendStatus"] = "passed"
                    steps += LiveProofStep.passed(
                        "group-notice",
                        sendNoticeDetail(targetSet, dispatch.result.statuses.mapNotNull { it.detail }),
                    )
                    true
                } else {
                    val detail = failed.detail ?: failed.state.name.lowercase()
                    statusFields["noticeSendStatus"] = classifyStatus(detail)
                    steps += LiveProofStep("group-notice", statusFields.getValue("noticeSendStatus"), detail)
                    markNotRunUntilLogout(detail, "notice-archive")
                    false
                }
            }
        }
    }

    private fun sendNoticeDetail(
        targetSet: GroupTargetSet,
        sentDetails: List<String>,
    ): String =
        if (sentDetails.isEmpty()) {
            "targets=${targetSet.selectedCount}"
        } else {
            "targets=${targetSet.selectedCount}; ${sentDetails.joinToString("; ")}"
        }

    private fun noticeFailed(detail: String): Boolean {
        statusFields["noticeSendStatus"] = classifyStatus(detail)
        steps += LiveProofStep("group-notice", statusFields.getValue("noticeSendStatus"), detail)
        markNotRunUntilLogout(detail, "notice-archive")
        return false
    }

    private fun noticeArchive(session: HostessSession, targetSet: GroupTargetSet): Boolean {
        val outcome = LiveProofNoticeArchiveVerifier(runtime.groupDirectoryService).read(
            session = session,
            targetSet = targetSet,
            expectedSubject = inputs.subject.orEmpty(),
            requireAttachment = true,
        )
        statusFields += outcome.statusFields
        steps += outcome.steps
        return if (outcome.passed) {
            true
        } else {
            markNotRunUntilLogout(outcome.failureReason ?: "notice archive proof failed", "cleanup")
            false
        }
    }

    private fun runCleanup() {
        cleanupStatus = "passed"
        steps += LiveProofStep.passed("cleanup", "authorised retained external notice proof")
    }

    private fun finishAfterLogout(session: HostessSession, reason: String? = null): CommandResult {
        runLogout(session)
        val status = terminalStatus()
        return finish(status, reason ?: "live proof ${status.wireValue}")
    }

    private fun runLogout(session: HostessSession) {
        when (val result = runtime.sessionService.logout(session)) {
            SessionLogoutResult.LoggedOut -> {
                statusFields["logoutStatus"] = "passed"
                statusFields["simulatorLogoutStatus"] = "passed"
                steps += LiveProofStep.passed("logout")
            }
            is SessionLogoutResult.Failure -> {
                terminalFailure = true
                statusFields["logoutStatus"] = "failed"
                statusFields["simulatorLogoutStatus"] = "failed"
                steps += LiveProofStep.failed(
                    "logout",
                    result.failure.redactedMessage ?: result.failure.reason.name.lowercase(),
                )
            }
        }
    }

    private fun markNotRunUntilLogout(detail: String, startAt: String) {
        LiveProofStep.notRunPlan(detail, startAt)
            .filterNot { it.name == "logout" }
            .forEach(steps::add)
    }

    private fun terminalStatus(): ProofReportStatus {
        if (cleanupStatus == "failed" || terminalFailure) {
            return ProofReportStatus.FAILED
        }
        val values = statusFields.values
        return when {
            "failed" in values -> ProofReportStatus.FAILED
            "transport_gap" in values -> ProofReportStatus.TRANSPORT_GAP
            "runtime_gap" in values -> ProofReportStatus.RUNTIME_GAP
            "proof_gap" in values -> ProofReportStatus.PROOF_GAP
            "blocked" in values -> ProofReportStatus.BLOCKED
            else -> ProofReportStatus.PASSED
        }
    }

    private fun classifyStatus(detail: String): String {
        return LiveProofStatusClassifier.classify(detail)
    }

    private fun finish(status: ProofReportStatus, reason: String): CommandResult {
        runtime.proofReportWriter.writeIfRequested(
            reportPath = reportPath,
            command = commandName,
            mode = CommandMode.LIVE.label(),
            status = status,
            statusFields = statusFields,
            inputs = reportInputs,
            results = steps.map(LiveProofStep::toReportMap),
            cleanupStatus = cleanupStatus,
            blockedReason = reason.takeIf { status == ProofReportStatus.BLOCKED },
        )
        output.line("live-proof live ${status.wireValue}: $reason")
        return if (status == ProofReportStatus.PASSED) CommandResult.SUCCESS else CommandResult.UNAVAILABLE
    }

    private companion object {
        val FORBIDDEN_TARGET: Regex = Regex("^(Omnivrz|Omniverse)$", RegexOption.IGNORE_CASE)
        const val REQUIRED_AGNI_START_LOCATION: String = "uri:London City&76&174&23"
    }
}
