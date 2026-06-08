package org.hostess.tools.cli.commands

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
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
    private var cleanupStatus = "not_applicable"
    private var terminalFailure = false

    fun run(): CommandResult = when (inputs.proofScope) {
        LiveProofScope.SIMULATOR_PRESENCE -> runSimulatorPresenceProof()
        LiveProofScope.NOTICE_ARCHIVE -> runNoticeArchiveProof()
        LiveProofScope.READ_GROUPS -> runReadGroupsProof()
        LiveProofScope.LOGIN_ONLY -> runLoginOnlyProof()
        LiveProofScope.INVENTORY_CATALOGUE -> runInventoryCatalogueProof()
        LiveProofScope.FULL -> LiveNoticeSendProofRunner(runtime, inputs, reportPath, output, commandName).run()
        LiveProofScope.UNSUPPORTED -> finish(ProofReportStatus.BLOCKED, "proof scope unsupported")
    }

    private fun runSimulatorPresenceProof(): CommandResult {
        val session = login(planCurrentGroupsOnFailure = false)
            ?: run {
                markSimulatorPresenceProofStepsNotRun("login blocked", includeLogout = true)
                return finish(ProofReportStatus.BLOCKED, "login blocked")
            }
        val presence = simulatorPresence(session)
        markReadOnlyPresenceProofStepsNotRun("simulator-presence scope")
        runLogout(session)
        return if (presence == null) {
            finish(terminalStatus(), "simulator presence unavailable")
        } else {
            finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
        }
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

    private fun runNoticeArchiveProof(): CommandResult {
        val session = login(planCurrentGroupsOnFailure = false)
            ?: run {
                markNoticeArchiveProofStepsNotRun("login blocked", includeLogout = true)
                return finish(ProofReportStatus.BLOCKED, "login blocked")
            }
        val groups = currentGroups(session, planMutationStepsOnFailure = false)
            ?: run {
                markNoticeArchiveAfterCurrentGroupsNotRun("current groups unavailable")
                runLogout(session)
                return finish(terminalStatus(), "current groups unavailable")
            }
        val targetSet = selectArchiveTargets(groups)
            ?: run {
                runLogout(session)
                return finish(terminalStatus(), "target selection failed")
            }
        val archiveAvailable = noticeArchive(session, targetSet)
        markNoticeArchiveMutationStepsNotRun("notice-archive scope")
        runLogout(session)
        return if (archiveAvailable) {
            finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
        } else {
            finish(terminalStatus(), "notice archive unavailable")
        }
    }

    private fun simulatorPresence(session: HostessSession): SimulatorPresenceProof? =
        when (val result = runtime.groupDirectoryService.simulatorPresence(session)) {
            is SimulatorPresenceProofResult.Success -> {
                applySimulatorPresenceFields(result.proof)
                steps += LiveProofStep.passed("simulator-presence", simulatorPresenceDetail(result.proof))
                result.proof
            }
            is SimulatorPresenceProofResult.Failure -> {
                applySimulatorPresenceFields(result.proof)
                val detail = result.proof.redactedMessage
                    ?: result.failure.redactedMessage
                    ?: result.failure.reason.name.lowercase()
                steps += LiveProofStep("simulator-presence", result.proof.simulatorPresenceStatus.reportValue, detail)
                null
            }
        }

    private fun selectArchiveTargets(groups: List<GroupMembership>): GroupTargetSet? =
        when (
            val selection = LiveProofTargetSelector(runtime.targetSelectionService)
                .select(groups, inputs.targetDisplayNames)
        ) {
            is LiveProofTargetSelection.Selected -> {
                steps += LiveProofStep.passed("select-targets", "targets=${selection.targetSet.selectedCount}")
                selection.targetSet
            }
            is LiveProofTargetSelection.Failed -> {
                statusFields["noticeArchiveStatus"] = "blocked"
                steps += LiveProofStep("select-targets", "blocked", selection.detail)
                markNoticeArchiveAfterTargetSelectionNotRun(selection.detail)
                null
            }
        }

    private fun noticeArchive(session: HostessSession, targetSet: GroupTargetSet): Boolean {
        statusFields["noticeArchiveTargetCount"] = targetSet.selectedCount.toString()
        var totalEntries = 0
        var firstFailureStatus: String? = null
        for (group in targetSet.selectedGroups) {
            when (val result = runtime.groupDirectoryService.noticeArchive(session, group)) {
                is GroupNoticeArchiveResult.Success -> {
                    totalEntries += result.entries.size
                    steps += LiveProofStep.passed(
                        "notice-archive",
                        "target=${group.displayName.value}; entries=${result.entries.size}; bodyEcho=not_run",
                    )
                }
                is GroupNoticeArchiveResult.Failure -> {
                    val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                    val status = classifyStatus(detail)
                    firstFailureStatus = firstFailureStatus ?: status
                    steps += LiveProofStep(
                        "notice-archive",
                        status,
                        "target=${group.displayName.value}; $detail",
                    )
                }
            }
        }
        if (firstFailureStatus != null) {
            statusFields["noticeArchiveStatus"] = firstFailureStatus
            return false
        }
        statusFields["noticeArchiveStatus"] = "passed"
        steps += LiveProofStep.passed(
            "notice-archive",
            "targets=${targetSet.selectedCount}; readable=${targetSet.selectedCount}; entries=$totalEntries; " +
                "bodyEcho=not_run",
        )
        return true
    }

    private fun runLoginOnlyProof(): CommandResult {
        val session = login(planCurrentGroupsOnFailure = false)
            ?: run {
                markLoginOnlyProofStepsNotRun("login blocked", includeLogout = true)
                return finish(ProofReportStatus.BLOCKED, "login blocked")
            }
        runLogout(session)
        markLoginOnlyProofStepsNotRun("login-only scope", includeLogout = false)
        return finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
    }

    private fun runInventoryCatalogueProof(): CommandResult {
        val session = login(planCurrentGroupsOnFailure = false)
            ?: run {
                markInventoryCatalogueProofStepsNotRun("login blocked", includeLogout = true)
                return finish(ProofReportStatus.BLOCKED, "login blocked")
            }
        val items = inventoryCatalogue(session)
        markSendProofStepsNotRun("inventory-catalogue scope", includeCleanup = true)
        runLogout(session)
        return if (items == null) {
            finish(terminalStatus(), "inventory catalogue unavailable")
        } else {
            finish(terminalStatus(), "live proof ${terminalStatus().wireValue}")
        }
    }

    private fun login(planCurrentGroupsOnFailure: Boolean = true): HostessSession? {
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
                if (planCurrentGroupsOnFailure) {
                    steps += LiveProofStep.notRunPlan(detail, "current-groups")
                }
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
                steps += LiveProofStep.passed("current-groups", currentGroupsDetail(result.groups))
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

    private fun currentGroupsDetail(groups: List<GroupMembership>): String {
        if (inputs.proofScope != LiveProofScope.READ_GROUPS || groups.isEmpty()) {
            return "groups=${groups.size}"
        }
        val displayNames = groups
            .map { it.displayName.value }
            .sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })
            .joinToString("|")
        return "groups=${groups.size}; displayNames=$displayNames"
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
                steps += LiveProofStep.passed("inventory-catalogue", inventoryCatalogueDetail(result.items))
                result.items
            }
            is InventoryItemListResult.Failure -> {
                val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                statusFields["inventoryCatalogueStatus"] = classifyStatus(detail)
                steps += LiveProofStep("inventory-catalogue", statusFields.getValue("inventoryCatalogueStatus"), detail)
                null
            }
        }

    private fun inventoryCatalogueDetail(items: List<InventoryItemDescriptor>): String {
        val displayNames = items
            .map { it.displayName.value }
            .sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })
            .joinToString("|")
        return "items=${items.size}; displayNames=$displayNames"
    }

    private fun markSendProofStepsNotRun(detail: String, includeCleanup: Boolean = false) {
        listOf(
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup".takeIf { includeCleanup },
        ).filterNotNull().forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markReadOnlyPresenceProofStepsNotRun(detail: String) {
        listOf(
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markLoginOnlyProofStepsNotRun(
        detail: String,
        includeLogout: Boolean,
    ) {
        listOf(
            "logout".takeIf { includeLogout },
            "simulator-presence",
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).filterNotNull().forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markSimulatorPresenceProofStepsNotRun(
        detail: String,
        includeLogout: Boolean,
    ) {
        listOf(
            "logout".takeIf { includeLogout },
            "simulator-presence",
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).filterNotNull().forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markNoticeArchiveProofStepsNotRun(
        detail: String,
        includeLogout: Boolean,
    ) {
        listOf(
            "logout".takeIf { includeLogout },
            "simulator-presence",
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).filterNotNull().forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markNoticeArchiveAfterCurrentGroupsNotRun(detail: String) {
        listOf(
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markNoticeArchiveAfterTargetSelectionNotRun(detail: String) {
        listOf(
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markNoticeArchiveMutationStepsNotRun(detail: String) {
        listOf(
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "cleanup",
        ).forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun markInventoryCatalogueProofStepsNotRun(
        detail: String,
        includeLogout: Boolean,
    ) {
        listOf(
            "logout".takeIf { includeLogout },
            "simulator-presence",
            "inventory-catalogue",
            "select-targets",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
        ).filterNotNull().forEach { step -> steps += LiveProofStep(step, "not_run", detail) }
    }

    private fun applySimulatorPresenceFields(proof: SimulatorPresenceProof) {
        statusFields["simulatorPresenceStatus"] = proof.simulatorPresenceStatus.reportValue
        statusFields["regionHandshakeStatus"] = proof.regionHandshakeStatus.reportValue
        statusFields["regionHandshakeReplyStatus"] = proof.regionHandshakeReplyStatus.reportValue
        statusFields["agentMovementStatus"] = proof.agentMovementStatus.reportValue
        statusFields["agentUpdateStatus"] = proof.agentUpdateStatus.reportValue
    }

    private fun simulatorPresenceDetail(proof: SimulatorPresenceProof): String =
        "pingReplies=${proof.pingReplies}"

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
