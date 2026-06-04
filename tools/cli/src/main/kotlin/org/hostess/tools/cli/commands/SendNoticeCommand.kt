package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.GroupListResult
import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.composition.CliCompositionRoot

class SendNoticeCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "send-notice"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        val subject = arguments.option("subject") ?: return usage(output, "missing subject")
        val targetNames = arguments.optionValues("target")
        if (targetNames.isEmpty()) {
            return usage(output, "missing target display name")
        }

        val runtime = compositionRoot.runtime(mode)
        val session = runtime.sessionProvider()
        val groups = when (val result = runtime.groupDirectoryService.currentGroups(session)) {
            is GroupListResult.Success -> result.groups
            is GroupListResult.Failure -> {
                output.line("send-notice ${mode.label()} failed: ${result.failure.redactedMessage ?: "groups unavailable"}")
                return CommandResult.UNAVAILABLE
            }
        }

        var targetSet = runtime.targetSelectionService.emptyTargetSet(groups)
        for (targetName in targetNames) {
            when (val selection = runtime.targetSelectionService.addTarget(targetSet, GroupDisplayName(targetName))) {
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
                session,
                draft,
                attachment = (attachmentResult as AttachmentCommandResult.Valid).attachment,
            )
        ) {
            is NoticeDispatchResult.Rejected -> {
                output.line("send-notice ${mode.label()} rejected: ${dispatch.validation}")
                CommandResult.USAGE_ERROR
            }
            is NoticeDispatchResult.Sent -> {
                output.line("send-notice ${mode.label()} attempted=${dispatch.result.statuses.size}")
                dispatch.result.statuses.forEach { status ->
                    output.line("${status.group.displayName.value}: ${status.state.name.lowercase()}")
                }
                CommandResult.SUCCESS
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
        output.line("usage: send-notice --mode fake|live --target <display-name> --subject <subject> --body <body>")
        return CommandResult.USAGE_ERROR
    }

    private sealed interface AttachmentCommandResult {
        data class Valid(val attachment: AttachmentRef?) : AttachmentCommandResult
        data object Invalid : AttachmentCommandResult
    }
}
