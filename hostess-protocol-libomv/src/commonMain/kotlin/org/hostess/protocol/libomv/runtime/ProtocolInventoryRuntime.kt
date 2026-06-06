package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.LibomvSessionIdentityResult
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.mapping.LoginInventoryRootsResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMapping
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMappingResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemMapping
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult

class ProtocolInventoryRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val capabilityUrlProvider: CapabilityUrlProvider? = null,
    private val inventorySource: InventoryRuntimeSource = InventoryRuntimeSource.unavailable(),
) {
    fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult {
        val context = when (val built = inventoryContext(session, CoreFailureReason.ATTACHMENT_NOT_FOUND)) {
            is InventoryContextResult.Failure ->
                return AttachmentResolutionResult.Failed(built.failure)
            is InventoryContextResult.Success -> built.context
        }
        return mapResult(
            result = inventorySource.resolveExistingAttachment(
                identity = context.identity,
                roots = context.roots,
                capabilityUrl = context.capabilityUrl,
                request = request,
            ),
            expectedKind = request.kind,
            mappingFailureReason = CoreFailureReason.ATTACHMENT_NOT_FOUND,
            mappingFailureMessage = "attachment kind mismatch",
        )
    }

    fun listItems(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult {
        val context = when (val built = inventoryContext(session, CoreFailureReason.INVENTORY_LIST_FAILED)) {
            is InventoryContextResult.Failure -> return InventoryItemListResult.Failure(built.failure)
            is InventoryContextResult.Success -> built.context
        }
        return when (val result = inventorySource.listItems(context.identity, context.roots, context.capabilityUrl, query)) {
            is InventoryRuntimeItemListResult.Failed ->
                InventoryItemListResult.Failure(CoreFailure(CoreFailureReason.INVENTORY_LIST_FAILED, result.message))
            is InventoryRuntimeItemListResult.Success -> InventoryItemListResult.Success(
                mapDescriptors(result.items, query),
            )
        }
    }

    private fun inventoryContext(
        session: HostessSession,
        failureReason: CoreFailureReason,
    ): InventoryContextResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure ->
                return InventoryContextResult.Failure(result.failure.copy(reason = failureReason))
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        val roots = when (val result = clientSession.inventoryRoots(session)) {
            is LoginInventoryRootsResult.Failure ->
                return InventoryContextResult.Failure(result.failure.copy(reason = failureReason))
            is LoginInventoryRootsResult.Success -> result.roots
        }
        if (roots.inventoryRootId.isNullOrBlank()) {
            return InventoryContextResult.Failure(CoreFailure(failureReason, "inventory root unavailable"))
        }
        val provider = capabilityUrlProvider ?: return InventoryContextResult.Failure(
            CoreFailure(failureReason, "inventory runtime unavailable"),
        )
        val capabilityUrl = when (val result = provider.requireUrl(
            identity,
            CapabilityName.FETCH_INVENTORY_DESCENDENTS2,
        )) {
            is CapabilityUrlResult.Ready -> result.url
            is CapabilityUrlResult.TransportGap ->
                return InventoryContextResult.Failure(CoreFailure(failureReason, result.redactedMessage))
            is CapabilityUrlResult.MappingGap ->
                return InventoryContextResult.Failure(CoreFailure(failureReason, result.redactedMessage))
        }
        return InventoryContextResult.Success(InventoryRuntimeContext(identity, roots, capabilityUrl))
    }

    private fun mapDescriptors(
        items: List<LibomvInventoryItemSnapshot>,
        query: InventoryItemQuery,
    ): List<InventoryItemDescriptor> = items
        .mapNotNull(LibomvInventoryItemMapping::descriptor)
        .filter { descriptor -> descriptor.kind in query.kinds }
        .filter { descriptor ->
            val needle = query.displayNameContains?.trim()?.takeIf(String::isNotBlank) ?: return@filter true
            descriptor.displayName.value.contains(needle, ignoreCase = true)
        }
        .sortedWith(compareBy<InventoryItemDescriptor> { it.displayName.value.lowercase() }.thenBy { it.itemId.value })

    private fun mapResult(
        result: InventoryRuntimeResult,
        expectedKind: AttachmentKind,
        mappingFailureReason: CoreFailureReason,
        mappingFailureMessage: String,
    ): AttachmentResolutionResult =
        when (result) {
            is InventoryRuntimeResult.Failed -> failure(result.reason, result.message)
            is InventoryRuntimeResult.Success -> mapSnapshot(
                result.snapshot,
                expectedKind,
                mappingFailureReason,
                mappingFailureMessage,
            )
        }

    private fun mapSnapshot(
        snapshot: LibomvAttachmentSnapshot,
        expectedKind: AttachmentKind,
        mappingFailureReason: CoreFailureReason,
        mappingFailureMessage: String,
    ): AttachmentResolutionResult =
        when (val mapped = LibomvAttachmentMapping.attachmentRef(snapshot, expectedKind)) {
            LibomvAttachmentMappingResult.Failure -> failure(mappingFailureReason, mappingFailureMessage)
            is LibomvAttachmentMappingResult.Success -> AttachmentResolutionResult.Resolved(mapped.attachment)
        }

    private fun failure(reason: CoreFailureReason, message: String): AttachmentResolutionResult.Failed =
        AttachmentResolutionResult.Failed(CoreFailure(reason, redactedMessage = message))
}

internal interface InventoryRuntimeSource {
    fun resolveExistingAttachment(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
        request: ExistingInventoryAttachment,
    ): InventoryRuntimeResult

    fun listItems(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
        query: InventoryItemQuery,
    ): InventoryRuntimeItemListResult

    companion object {
        fun unavailable(): InventoryRuntimeSource = object : InventoryRuntimeSource {
            override fun resolveExistingAttachment(
                identity: LibomvSessionIdentity,
                roots: LoginInventoryRoots,
                capabilityUrl: CapabilityUrl,
                request: ExistingInventoryAttachment,
            ): InventoryRuntimeResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_NOT_FOUND,
                "attachment runtime unavailable",
            )

            override fun listItems(
                identity: LibomvSessionIdentity,
                roots: LoginInventoryRoots,
                capabilityUrl: CapabilityUrl,
                query: InventoryItemQuery,
            ): InventoryRuntimeItemListResult = InventoryRuntimeItemListResult.Failed("inventory runtime unavailable")
        }
    }
}

internal sealed interface InventoryRuntimeResult {
    data class Success(val snapshot: LibomvAttachmentSnapshot) : InventoryRuntimeResult
    data class Failed(val reason: CoreFailureReason, val message: String) : InventoryRuntimeResult
}

internal sealed interface InventoryRuntimeItemListResult {
    data class Success(val items: List<LibomvInventoryItemSnapshot>) : InventoryRuntimeItemListResult
    data class Failed(val message: String) : InventoryRuntimeItemListResult
}

private data class InventoryRuntimeContext(
    val identity: LibomvSessionIdentity,
    val roots: LoginInventoryRoots,
    val capabilityUrl: CapabilityUrl,
)

private sealed interface InventoryContextResult {
    data class Success(val context: InventoryRuntimeContext) : InventoryContextResult
    data class Failure(val failure: CoreFailure) : InventoryContextResult
}
