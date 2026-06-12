package org.gem.tools.cli.commands

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDispatchResult
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.SessionLoginResult
import org.gem.tools.cli.CliCommand
import org.gem.tools.cli.CliOutput
import org.gem.tools.cli.CommandArguments
import org.gem.tools.cli.CommandMode
import org.gem.tools.cli.CommandResult
import org.gem.tools.cli.composition.CliRuntime
import org.gem.tools.cli.composition.CliCompositionRoot
import org.gem.tools.cli.report.ProofReportStatus
import org.gem.tools.cli.report.ProofReportWriter

class SendNoticeCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "send-notice"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        staleNoticeTotalsOption(arguments)?.let { return usage(output, it) }
        val mode = arguments.mode()
        if (mode == CommandMode.LIVE) {
            return blockLiveSend(arguments, output)
        }
        val subject = arguments.option("subject") ?: return usage(output, "missing subject")
        val targetNames = arguments.optionValues("target")
        if (targetNames.isEmpty()) {
            return usage(output, "missing target display name")
        }

        val runtime = compositionRoot.runtime(mode)
        val session = loginSession(arguments, runtime, mode, targetNames, subject, output)
            ?: return CommandResult.UNAVAILABLE
        val groups = when (val result = runtime.groupDirectoryService.currentGroups(session)) {
            is GroupListResult.Success -> result.groups
            is GroupListResult.Failure -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = mapOf("sendNoticeStatus" to "failed"),
                    inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()),
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

        val attachmentResult = resolveAttachment(arguments, runtime, session, output)
        if (attachmentResult is AttachmentCommandResult.Invalid) {
            return CommandResult.USAGE_ERROR
        }
        val validAttachment = when (attachmentResult) {
            is AttachmentCommandResult.Valid -> attachmentResult
            is AttachmentCommandResult.Invalid -> return CommandResult.USAGE_ERROR
        }

        val draft = runtime.noticeDraftService.createDraft(
            subject = subject,
            message = arguments.option("body").orEmpty(),
            targetSet = targetSet,
            attachments = listOf(validAttachment.request),
        )

        return when (
            val dispatch = runtime.noticeDispatchService.dispatch(
                session = session,
                draft = draft,
                attachment = validAttachment.attachment,
            )
        ) {
            is NoticeDispatchResult.Rejected -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = mapOf("sendNoticeStatus" to "failed"),
                    inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()),
                    blockedReason = dispatch.validation.toString(),
                )
                output.line("send-notice ${mode.label()} rejected: ${dispatch.validation}")
                CommandResult.USAGE_ERROR
            }
            is NoticeDispatchResult.Sent -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.PASSED,
                    statusFields = mapOf("sendNoticeStatus" to "passed"),
                    inputs = sendInputs(mode.label(), targetNames, subject, arguments.option("body").orEmpty()),
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
    ): GemSession? {
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
        session: GemSession,
        output: CliOutput,
    ): AttachmentCommandResult {
        val kind = arguments.option("attachment-kind")
        if (kind == null) {
            output.line("send-notice usage error: missing attachment-kind")
            return AttachmentCommandResult.Invalid
        }
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
            is AttachmentResolutionResult.Resolved -> AttachmentCommandResult.Valid(request, result.attachment)
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
                "--attachment-kind <landmark|texture> --attachment-id <inventory-item-id> --report <path>",
        )
        return CommandResult.USAGE_ERROR
    }

    private fun staleNoticeTotalsOption(arguments: CommandArguments): String? = when {
        arguments.has("ledger") -> "ledger is no longer supported; local notice totals were removed"
        arguments.has("recipient-count") -> "recipient-count is no longer supported; local notice totals were removed"
        arguments.has("recipient-count-source") -> "recipient-count-source is no longer supported; local notice totals were removed"
        else -> null
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

    private fun List<org.gem.core.domain.GroupSendStatus>.toReportRows(): List<Map<String, String>> =
        map { status ->
            mapOf(
                "displayName" to status.group.displayName.value,
                "state" to status.state.name.lowercase(),
                "detail" to status.detail.orEmpty(),
            )
        }

    private sealed interface AttachmentCommandResult {
        data class Valid(
            val request: ExistingInventoryAttachment,
            val attachment: AttachmentRef,
        ) : AttachmentCommandResult
        data object Invalid : AttachmentCommandResult
    }

    private companion object {
        const val LIVE_BLOCKED_REASON: String = "live send is only available through live-proof --proof-scope full"
    }
}
