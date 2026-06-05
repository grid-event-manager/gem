package org.hostess.tools.cli.commands

import java.time.Duration
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.AttachmentRequest
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.domain.AccountLabel
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.report.ProofReportStatus

internal class LiveProofRunner(
    private val runtime: CliRuntime,
    private val inputs: LiveProofInputs,
    private val reportPath: String,
    private val output: CliOutput,
    private val commandName: String,
) {
    private val steps = mutableListOf(LiveProofStep.passed("validate-inputs"))
    private val statusFields = LiveProofStep.statusFields().toMutableMap()
    private val noticeCompliance = LiveProofNoticeCompliance(inputs)
    private var cleanupStatus = "not_applicable"
    private var terminalFailure = false

    fun run(): CommandResult = when (inputs.proofScope) {
        LiveProofScope.READ_GROUPS -> runReadGroupsProof()
        LiveProofScope.FULL -> runFullProof()
        LiveProofScope.UNSUPPORTED -> finish(ProofReportStatus.BLOCKED, "proof scope unsupported")
    }

    private fun runFullProof(): CommandResult {
        statusFields += noticeCompliance.reportStatusFields(null)
        val session = login() ?: return finish(ProofReportStatus.BLOCKED, "login blocked")
        val groups = currentGroups(session) ?: return finish(terminalStatus(), "current groups unavailable")
        val targetSet = selectTargets(groups) ?: return finish(ProofReportStatus.FAILED, "target selection failed")
        if (!sendPlainNotice(session, targetSet)) {
            return finish(terminalStatus(), "plain notice failed")
        }

        runAttachmentProof(
            statusKey = "landmarkAttachmentStatus",
            resolveStep = "landmark-attachment",
            sendStep = "landmark-notice",
            missingReason = "landmark fixture unavailable",
            request = inputs.landmarkRequest(),
            session = session,
            targetSet = targetSet,
        )
        runAttachmentProof(
            statusKey = "textureAttachmentStatus",
            resolveStep = "texture-attachment",
            sendStep = "texture-notice",
            missingReason = "texture fixture unavailable",
            request = inputs.textureRequest(),
            session = session,
            targetSet = targetSet,
        )
        runBulkProof(session, targetSet)
        runCleanup()
        runLogout(session)

        return finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
    }

    private fun runReadGroupsProof(): CommandResult {
        val session = login() ?: return finish(ProofReportStatus.BLOCKED, "login blocked")
        val groups = currentGroups(session, planMutationStepsOnFailure = false)
        markSendProofStepsNotRun("read-groups scope")
        runLogout(session)
        return if (groups == null) {
            finish(terminalStatus(), "current groups unavailable")
        } else {
            finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
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
                steps += LiveProofStep.notRunPlan(detail, "current-groups")
                null
            }
        }
    }

    private fun currentGroups(
        session: HostessSession,
        planMutationStepsOnFailure: Boolean = true,
    ): List<GroupMembership>? =
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
                if (planMutationStepsOnFailure) {
                    steps += LiveProofStep.notRunPlan(detail, "select-targets")
                }
                null
            }
        }

    private fun markSendProofStepsNotRun(detail: String) {
        listOf(
            "select-targets",
            "plain-notice",
            "landmark-attachment",
            "landmark-notice",
            "texture-attachment",
            "texture-notice",
            "bulk-notice",
        ).forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun selectTargets(groups: List<GroupMembership>): GroupTargetSet? {
        var targetSet = runtime.targetSelectionService.emptyTargetSet(groups)
        for (displayName in inputs.targetDisplayNames) {
            targetSet = when (val result = runtime.targetSelectionService.addTargetByDisplayName(targetSet, displayName)) {
                is TargetSelectionResult.Changed -> result.targetSet
                is TargetSelectionResult.Unchanged -> result.targetSet
                else -> {
                    steps += LiveProofStep.failed("select-targets", "target group display name unavailable")
                    steps += LiveProofStep.notRunPlan("target group display name unavailable", "plain-notice")
                    return null
                }
            }
        }
        if (targetSet.isEmpty()) {
            steps += LiveProofStep.failed("select-targets", "target group display name unavailable")
            steps += LiveProofStep.notRunPlan("target group display name unavailable", "plain-notice")
            return null
        }
        steps += LiveProofStep.passed("select-targets", "targets=${targetSet.selectedCount}")
        return targetSet
    }

    private fun sendPlainNotice(session: HostessSession, targetSet: GroupTargetSet): Boolean {
        val detail = dispatchFailure(session, noticeDraft(targetSet), targetSet, attachment = null)
        if (detail == null) {
            statusFields["plainNoticeStatus"] = "passed"
            steps += LiveProofStep.passed("plain-notice")
            return true
        }
        statusFields["plainNoticeStatus"] = classifyStatus(detail)
        steps += LiveProofStep("plain-notice", statusFields.getValue("plainNoticeStatus"), detail)
        steps += LiveProofStep.notRunPlan(detail, "landmark-attachment")
        return false
    }

    private fun runAttachmentProof(
        statusKey: String,
        resolveStep: String,
        sendStep: String,
        missingReason: String,
        request: AttachmentRequest?,
        session: HostessSession,
        targetSet: GroupTargetSet,
    ) {
        if (request == null) {
            statusFields[statusKey] = "blocked"
            steps += LiveProofStep(resolveStep, "blocked", missingReason)
            steps += LiveProofStep(sendStep, "not_run", missingReason)
            return
        }
        val attachment = when (val result = runtime.attachmentService.resolveAttachment(session, request)) {
            is AttachmentResolutionResult.Resolved -> {
                steps += LiveProofStep.passed(resolveStep)
                result.attachment
            }
            is AttachmentResolutionResult.Failed -> {
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                statusFields[statusKey] = classifyStatus(detail)
                steps += LiveProofStep(resolveStep, statusFields.getValue(statusKey), detail)
                steps += LiveProofStep(sendStep, "not_run", detail)
                return
            }
        }
        val detail = dispatchFailure(session, noticeDraft(targetSet), targetSet, attachment)
        if (detail == null) {
            statusFields[statusKey] = "passed"
            steps += LiveProofStep.passed(sendStep)
        } else {
            statusFields[statusKey] = classifyStatus(detail)
            steps += LiveProofStep(sendStep, statusFields.getValue(statusKey), detail)
        }
    }

    private fun runBulkProof(session: HostessSession, targetSet: GroupTargetSet) {
        val bulkTargetSet = boundedBulkTargetSet(targetSet)
        if (bulkTargetSet == null) {
            val reason = "at least two authorised target groups not configured"
            statusFields["bulkNoticeStatus"] = "blocked"
            steps += LiveProofStep("bulk-notice", "blocked", reason)
            return
        }
        val detail = dispatchFailure(
            session = session,
            draft = noticeDraft(bulkTargetSet),
            targetSet = bulkTargetSet,
            attachment = null,
            pacingPolicy = PacingPolicy(Duration.ofMillis(inputs.bulkDelayMs?.coerceAtLeast(0L) ?: 0L)),
        )
        if (detail == null) {
            statusFields["bulkNoticeStatus"] = "passed"
            steps += LiveProofStep.passed("bulk-notice", "targets=${bulkTargetSet.selectedCount}")
        } else {
            statusFields["bulkNoticeStatus"] = classifyStatus(detail)
            steps += LiveProofStep("bulk-notice", statusFields.getValue("bulkNoticeStatus"), detail)
        }
    }

    private fun boundedBulkTargetSet(targetSet: GroupTargetSet): GroupTargetSet? {
        val limit = inputs.bulkLimit ?: targetSet.selectedCount
        if (limit < 2 || targetSet.selectedCount < 2) {
            return null
        }
        val groups = targetSet.selectedGroups.take(limit)
        return when (val result = GroupTargetSet.from(groups).addAllSendable()) {
            is TargetSelectionResult.Changed -> result.targetSet
            is TargetSelectionResult.Unchanged -> result.targetSet.takeUnless(GroupTargetSet::isEmpty)
            else -> null
        }
    }

    private fun runCleanup() {
        if (inputs.cleanupModeValue() == "retain-authorised" && inputs.retentionNote.isNullOrBlank()) {
            cleanupStatus = "failed"
            steps += LiveProofStep.failed("cleanup", "retention note required")
            return
        }
        cleanupStatus = "passed"
        steps += LiveProofStep.passed("cleanup", inputs.cleanupModeValue())
    }

    private fun runLogout(session: HostessSession) {
        when (val result = runtime.sessionService.logout(session)) {
            SessionLogoutResult.LoggedOut -> {
                statusFields["logoutStatus"] = "passed"
                steps += LiveProofStep.passed("logout")
            }
            is SessionLogoutResult.Failure -> {
                terminalFailure = true
                statusFields["logoutStatus"] = "failed"
                steps += LiveProofStep.failed(
                    "logout",
                    result.failure.redactedMessage ?: result.failure.reason.name.lowercase(),
                )
            }
        }
    }

    private fun dispatchFailure(
        session: HostessSession,
        draft: NoticeDraft,
        targetSet: GroupTargetSet,
        attachment: AttachmentRef? = null,
        pacingPolicy: PacingPolicy = PacingPolicy.NONE,
    ): String? {
        val complianceRequest = try {
            noticeCompliance.request(targetSet)
        } catch (exception: IllegalArgumentException) {
            return "blocked: ${exception.message ?: "notice compliance invalid"}"
        }

        return when (
            val result = runtime.noticeDispatchService.dispatch(
                session = session,
                draft = draft,
                compliance = complianceRequest,
                pacingPolicy = pacingPolicy,
                attachment = attachment,
            )
        ) {
            is NoticeDispatchResult.Rejected -> "notice draft rejected"
            is NoticeDispatchResult.ComplianceRejected -> {
                statusFields += noticeCompliance.reportStatusFields(result.decision.receipt)
                "blocked: ${result.decision.receipt.reasonCode}"
            }
            is NoticeDispatchResult.ComplianceRecordFailed -> {
                statusFields += noticeCompliance.reportStatusFields(result.complianceReceipt)
                result.complianceReceipt.reasonCode
            }
            is NoticeDispatchResult.Sent ->
                result.result.statuses
                    .firstOrNull { it.state != GroupSendState.SENT }
                    ?.let { it.detail ?: it.state.name.lowercase() }
                    .also {
                        statusFields += noticeCompliance.reportStatusFields(result.complianceReceipt)
                    }
        }
    }

    private fun noticeDraft(targetSet: GroupTargetSet): NoticeDraft =
        NoticeDraft(inputs.subject.orEmpty(), inputs.body.orEmpty(), targetSet)

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
        val lower = detail.lowercase()
        return when {
            "transport" in lower -> "transport_gap"
            "runtime" in lower || "unavailable" in lower -> "runtime_gap"
            "proof" in lower -> "proof_gap"
            "blocked" in lower -> "blocked"
            else -> "failed"
        }
    }

    private fun finish(status: ProofReportStatus, reason: String): CommandResult {
        runtime.proofReportWriter.writeIfRequested(
            reportPath = reportPath,
            command = commandName,
            mode = CommandMode.LIVE.label(),
            status = status,
            statusFields = statusFields,
            inputs = inputs.toReportInputs(CommandMode.LIVE),
            results = steps.map(LiveProofStep::toReportMap),
            cleanupStatus = cleanupStatus,
            blockedReason = reason.takeIf { status == ProofReportStatus.BLOCKED },
        )
        output.line("live-proof live ${status.wireValue}: $reason")
        return if (status == ProofReportStatus.PASSED) CommandResult.SUCCESS else CommandResult.UNAVAILABLE
    }
}
