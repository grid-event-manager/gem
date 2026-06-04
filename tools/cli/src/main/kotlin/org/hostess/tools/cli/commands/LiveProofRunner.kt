package org.hostess.tools.cli.commands

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
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
    fun run(): CommandResult {
        val steps = mutableListOf(LiveProofStep.passed("validate-inputs"))
        val loginRequest = LoginRequest(
            accountLabel = AccountLabel(inputs.account.orEmpty()),
            credentialHandle = CredentialHandle(inputs.credentialHandle.orEmpty()),
        )

        val session = when (val login = runtime.sessionService.login(loginRequest)) {
            is SessionLoginResult.Success -> {
                steps += LiveProofStep.passed("login")
                login.session
            }
            is SessionLoginResult.Failure -> {
                val reason = login.failure.redactedMessage ?: login.failure.reason.name.lowercase()
                writeTerminalReport(ProofReportStatus.FAILED, steps, "login", reason)
                output.line("live-proof live failed: $reason")
                return CommandResult.UNAVAILABLE
            }
        }

        val groups = when (val result = runtime.groupDirectoryService.currentGroups(session)) {
            is GroupListResult.Success -> {
                steps += LiveProofStep.passed("list-groups", "groups=${result.groups.size}")
                result.groups
            }
            is GroupListResult.Failure -> {
                val reason = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                writeTerminalReport(ProofReportStatus.FAILED, steps, "list-groups", reason)
                output.line("live-proof live failed: $reason")
                return CommandResult.UNAVAILABLE
            }
        }

        val targetSet = when (val selected = runtime.targetSelectionService
            .addTarget(GroupTargetSet.from(groups), GroupDisplayName(inputs.targetDisplayName.orEmpty()))) {
            is TargetSelectionResult.Changed -> {
                steps += LiveProofStep.passed("select-group")
                selected.targetSet
            }
            else -> {
                val reason = "target group display name is unavailable or not sendable"
                writeTerminalReport(ProofReportStatus.FAILED, steps, "select-group", reason)
                output.line("live-proof live failed: $reason")
                return CommandResult.UNAVAILABLE
            }
        }

        val plainDraft = NoticeDraft(inputs.subject.orEmpty(), inputs.body.orEmpty(), targetSet)
        val plainFailed = when (val result = runtime.noticeDispatchService.dispatch(session, plainDraft)) {
            is NoticeDispatchResult.Rejected -> "plain notice rejected"
            is NoticeDispatchResult.Sent ->
                result.result.statuses.firstOrNull { it.state == GroupSendState.FAILED }?.detail
        }
        if (plainFailed != null) {
            writeTerminalReport(ProofReportStatus.FAILED, steps, "send-plain-notice", plainFailed)
            output.line("live-proof live failed: $plainFailed")
            return CommandResult.UNAVAILABLE
        }
        steps += LiveProofStep.passed("send-plain-notice")

        val attachmentRequest = inputs.attachmentRequest()
        if (attachmentRequest == null) {
            val reason = "attachment input unavailable"
            val results = steps + LiveProofStep.blockedPlan("resolve-attachment", reason)
            runtime.proofReportWriter.writeIfRequested(
                reportPath = reportPath,
                command = commandName,
                mode = CommandMode.LIVE.label(),
                status = ProofReportStatus.BLOCKED,
                inputs = inputs.toReportInputs(CommandMode.LIVE),
                results = results.map(LiveProofStep::toReportMap),
                blockedReason = reason,
            )
            output.line("live-proof live blocked: $reason")
            return CommandResult.UNAVAILABLE
        }

        val attachment = when (val result = runtime.attachmentService.resolveAttachment(session, attachmentRequest)) {
            is AttachmentResolutionResult.Resolved -> {
                steps += LiveProofStep.passed("resolve-attachment")
                result.attachment
            }
            is AttachmentResolutionResult.Failed -> {
                val reason = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                writeTerminalReport(ProofReportStatus.FAILED, steps, "resolve-attachment", reason)
                output.line("live-proof live failed: $reason")
                return CommandResult.UNAVAILABLE
            }
        }

        val attachmentFailed = when (val result = runtime.noticeDispatchService.dispatch(
            session,
            plainDraft,
            attachment = attachment,
        )) {
            is NoticeDispatchResult.Rejected -> "attachment notice rejected"
            is NoticeDispatchResult.Sent ->
                result.result.statuses.firstOrNull { it.state == GroupSendState.FAILED }?.detail
        }
        if (attachmentFailed != null) {
            writeTerminalReport(ProofReportStatus.FAILED, steps, "send-attachment-notice", attachmentFailed)
            output.line("live-proof live failed: $attachmentFailed")
            return CommandResult.UNAVAILABLE
        }
        steps += LiveProofStep.passed("send-attachment-notice")

        val bulkReason = "at least two authorised target groups not configured"
        val results = steps + LiveProofStep.notRunPlan(bulkReason, startAt = "send-bulk-notice")
        runtime.proofReportWriter.writeIfRequested(
            reportPath = reportPath,
            command = commandName,
            mode = CommandMode.LIVE.label(),
            status = ProofReportStatus.BLOCKED,
            inputs = inputs.toReportInputs(CommandMode.LIVE),
            results = results.map(LiveProofStep::toReportMap),
            cleanupStatus = "not_applicable",
            blockedReason = bulkReason,
        )
        output.line("live-proof live blocked: $bulkReason")
        return CommandResult.UNAVAILABLE
    }

    private fun writeTerminalReport(
        status: ProofReportStatus,
        priorSteps: List<LiveProofStep>,
        failedStep: String,
        reason: String,
    ) {
        val results = priorSteps +
            LiveProofStep.failed(failedStep, reason) +
            LiveProofStep.notRunPlan(reason, startAt = LiveProofStep.after(failedStep))
        runtime.proofReportWriter.writeIfRequested(
            reportPath = reportPath,
            command = commandName,
            mode = CommandMode.LIVE.label(),
            status = status,
            inputs = inputs.toReportInputs(CommandMode.LIVE),
            results = results.map(LiveProofStep::toReportMap),
            cleanupStatus = "not_applicable",
        )
    }
}
