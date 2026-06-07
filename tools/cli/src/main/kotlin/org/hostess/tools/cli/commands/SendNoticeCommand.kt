package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.SessionLoginResult
import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.composition.CliCompositionRoot
import org.hostess.tools.cli.report.ProofReportStatus
import org.hostess.tools.cli.report.ProofReportWriter

class SendNoticeCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "send-notice"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        if (mode == CommandMode.LIVE) {
            return blockLiveSend(arguments, output)
        }
        val complianceArguments = NoticeComplianceArguments(arguments, mode)
        val subject = arguments.option("subject") ?: return usage(output, "missing subject")
        val targetNames = arguments.optionValues("target")
        if (targetNames.isEmpty()) {
            return usage(output, "missing target display name")
        }

        val runtime = compositionRoot.runtime(mode, complianceArguments.ledgerPath())
        val session = loginSession(arguments, runtime, mode, targetNames, subject, output)
            ?: return CommandResult.UNAVAILABLE
        val groups = when (val result = runtime.groupDirectoryService.currentGroups(session)) {
            is GroupListResult.Success -> result.groups
            is GroupListResult.Failure -> {
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    complianceArguments.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = complianceArguments.reportStatusFields(null),
                    inputs = inputs,
                    blockedReason = result.failure.redactedMessage,
                )
                output.line("send-notice ${mode.label()} failed: ${result.failure.redactedMessage ?: "groups unavailable"}")
                return CommandResult.UNAVAILABLE
            }
        }

        var targetSet = runtime.targetSelectionService.emptyTargetSet(groups)
        for (targetName in targetNames) {
            when (val selection = runtime.targetSelectionService.addTargetByDisplayName(targetSet, targetName)) {
                is TargetSelectionResult.Changed -> targetSet = selection.targetSet
                is TargetSelectionResult.Unchanged -> targetSet = selection.targetSet
                is TargetSelectionResult.NoSuchGroup -> return usage(output, "unknown group display name: ${selection.displayName.value}")
                is TargetSelectionResult.AmbiguousDisplayName -> return usage(output, "ambiguous group display name: ${selection.displayName.value}")
                is TargetSelectionResult.CannotSendNotice -> return usage(
                    output,
                    "group cannot send notices: ${selection.group.displayName.value}",
                )
            }
        }

        val missingCompliance = complianceArguments.missingRequiredFields(sendMayOccur = true)
        if (missingCompliance.isNotEmpty()) {
            return usage(output, "missing notice compliance input: ${missingCompliance.joinToString(", ")}")
        }
        val complianceRequest = try {
            complianceArguments.request(targetSet)
        } catch (exception: IllegalArgumentException) {
            return usage(output, exception.message ?: "notice compliance invalid")
        }

        val draft = runtime.noticeDraftService.createDraft(
            subject = subject,
            message = arguments.option("body").orEmpty(),
            targetSet = targetSet,
        )
        val attachmentResult = resolveAttachment(arguments, runtime, session, output)
        if (attachmentResult is AttachmentCommandResult.Invalid) {
            return CommandResult.USAGE_ERROR
        }

        return when (
            val dispatch = runtime.noticeDispatchService.dispatch(
                session = session,
                draft = draft,
                compliance = complianceRequest,
                attachment = (attachmentResult as AttachmentCommandResult.Valid).attachment,
            )
        ) {
            is NoticeDispatchResult.Rejected -> {
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    complianceArguments.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = complianceArguments.reportStatusFields(null),
                    inputs = inputs,
                    blockedReason = dispatch.validation.toString(),
                )
                output.line("send-notice ${mode.label()} rejected: ${dispatch.validation}")
                CommandResult.USAGE_ERROR
            }
            is NoticeDispatchResult.ComplianceRejected -> {
                val receipt = dispatch.decision.receipt
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    complianceArguments.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.BLOCKED,
                    statusFields = complianceArguments.reportStatusFields(receipt),
                    inputs = inputs,
                    blockedReason = receipt.reasonCode,
                )
                output.line("send-notice ${mode.label()} blocked: ${receipt.reasonCode}")
                CommandResult.UNAVAILABLE
            }
            is NoticeDispatchResult.ComplianceRecordFailed -> {
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    complianceArguments.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = complianceArguments.reportStatusFields(dispatch.complianceReceipt),
                    inputs = inputs,
                    results = dispatch.result.statuses.toReportRows(),
                    blockedReason = dispatch.complianceReceipt.reasonCode,
                )
                output.line("send-notice ${mode.label()} failed: ${dispatch.complianceReceipt.reasonCode}")
                CommandResult.UNAVAILABLE
            }
            is NoticeDispatchResult.Sent -> {
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    complianceArguments.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.PASSED,
                    statusFields = complianceArguments.reportStatusFields(dispatch.complianceReceipt),
                    inputs = inputs,
                    results = dispatch.result.statuses.toReportRows(),
                )
                output.line("send-notice ${mode.label()} attempted=${dispatch.result.statuses.size}")
                dispatch.result.statuses.forEach { status ->
                    output.line("${status.group.displayName.value}: ${status.state.name.lowercase()}")
                }
                CommandResult.SUCCESS
            }
        }
    }

    private fun blockLiveSend(arguments: CommandArguments, output: CliOutput): CommandResult {
        ProofReportWriter().writeIfRequested(
            reportPath = arguments.option("report"),
            command = name,
            mode = CommandMode.LIVE.label(),
            status = ProofReportStatus.BLOCKED,
            statusFields = mapOf("sendNoticeStatus" to "blocked"),
            inputs = mapOf(
                "mode" to CommandMode.LIVE.label(),
                "targetCount" to arguments.optionValues("target").size.toString(),
                "subjectLength" to arguments.option("subject").orEmpty().length.toString(),
                "bodyLength" to arguments.option("body").orEmpty().length.toString(),
            ),
            blockedReason = LIVE_BLOCKED_REASON,
        )
        output.line("send-notice live blocked: $LIVE_BLOCKED_REASON")
        return CommandResult.UNAVAILABLE
    }

    private fun loginSession(
        arguments: CommandArguments,
        runtime: CliRuntime,
        mode: CommandMode,
        targetNames: List<String>,
        subject: String,
        output: CliOutput,
    ): HostessSession? {
        val request = CommandLoginRequest.from(arguments, mode)
        if (request == null) {
            usage(output, "missing account or credential handle")
            return null
        }
        val loginCompliance = LoginComplianceArguments(arguments, mode)
        return when (
            val login = runtime.sessionService.login(
                request,
                loginCompliance.request(request.accountLabel.value),
            )
        ) {
            is SessionLoginResult.Success -> login.session
            is SessionLoginResult.Failure -> {
                val inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()) +
                    loginCompliance.reportInputs()
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = loginCompliance.statusFields(),
                    inputs = inputs,
                    blockedReason = login.failure.redactedMessage,
                )
                output.line("send-notice ${mode.label()} failed: ${login.failure.redactedMessage ?: "login unavailable"}")
                null
            }
        }
    }

    private fun resolveAttachment(
        arguments: CommandArguments,
        runtime: CliRuntime,
        session: HostessSession,
        output: CliOutput,
    ): AttachmentCommandResult {
        val kind = arguments.option("attachment-kind") ?: return AttachmentCommandResult.Valid(null)
        val itemId = arguments.option("attachment-id")
        if (itemId == null) {
            output.line("send-notice usage error: missing attachment-id")
            return AttachmentCommandResult.Invalid
        }
        val attachmentKind = attachmentKind(kind)
        if (attachmentKind == null) {
            output.line("send-notice usage error: unknown attachment-kind")
            return AttachmentCommandResult.Invalid
        }
        val request = ExistingInventoryAttachment(attachmentKind, InventoryItemId(itemId))

        return when (val result = runtime.attachmentService.resolveAttachment(session, request)) {
            is AttachmentResolutionResult.Resolved -> AttachmentCommandResult.Valid(result.attachment)
            is AttachmentResolutionResult.Failed -> {
                output.line("send-notice attachment failed: ${result.failure.redactedMessage ?: "unavailable"}")
                AttachmentCommandResult.Invalid
            }
        }
    }

    private fun attachmentKind(value: String): AttachmentKind? = when (value.lowercase()) {
        "landmark" -> AttachmentKind.LANDMARK
        "texture" -> AttachmentKind.TEXTURE
        else -> null
    }

    private fun usage(output: CliOutput, reason: String): CommandResult {
        output.line("send-notice usage error: $reason")
        output.line(
            "usage: send-notice --mode fake --target <display-name> --subject <subject> --body <body> " +
                "--operator <label> --recipient-count <display-name=count> " +
                "--recipient-count-source operator-acknowledged|authoritative --ledger <path> --report <path>",
        )
        return CommandResult.USAGE_ERROR
    }

    private fun sendInputs(
        mode: String,
        targets: List<String>,
        subject: String,
        body: String,
    ): Map<String, String> = mapOf(
        "mode" to mode,
        "targetDisplayNames" to targets.joinToString(", "),
        "subject" to subject,
        "bodyLength" to body.length.toString(),
    )

    private fun List<org.hostess.core.domain.GroupSendStatus>.toReportRows(): List<Map<String, String>> =
        map { status ->
            mapOf(
                "displayName" to status.group.displayName.value,
                "state" to status.state.name.lowercase(),
                "detail" to status.detail.orEmpty(),
            )
        }

    private sealed interface AttachmentCommandResult {
        data class Valid(val attachment: AttachmentRef?) : AttachmentCommandResult
        data object Invalid : AttachmentCommandResult
    }

    private companion object {
        const val LIVE_BLOCKED_REASON: String = "live send is only available through live-proof --proof-scope full"
    }
}
