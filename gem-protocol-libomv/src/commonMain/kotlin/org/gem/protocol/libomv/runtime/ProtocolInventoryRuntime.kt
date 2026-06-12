package org.gem.protocol.libomv.runtime

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryFolderDescriptor
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.LibomvSessionIdentityResult
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import org.gem.protocol.libomv.mapping.LoginInventoryRootsResult
import org.gem.protocol.libomv.mapping.LibomvAttachmentMapping
import org.gem.protocol.libomv.mapping.LibomvAttachmentMappingResult
import org.gem.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.gem.protocol.libomv.mapping.LibomvInventoryFolderMapping
import org.gem.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.gem.protocol.libomv.mapping.LibomvInventoryItemMapping
import org.gem.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.gem.protocol.libomv.transport.CapabilityName
import org.gem.protocol.libomv.transport.CapabilityUrl
import org.gem.protocol.libomv.transport.CapabilityUrlProvider
import org.gem.protocol.libomv.transport.CapabilityUrlResult

class ProtocolInventoryRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val capabilityUrlProvider: CapabilityUrlProvider? = null,
    private val inventorySource: InventoryRuntimeSource = InventoryRuntimeSource.unavailable(),
) {
    fun resolveExistingAttachment(
        session: GemSession,
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
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult =
        when (val result = listDirectory(session, query)) {
            is InventoryDirectoryListResult.Success -> InventoryItemListResult.Success(result.listing.items)
            is InventoryDirectoryListResult.Failure -> InventoryItemListResult.Failure(result.failure)
        }

    fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult {
        val context = when (val built = inventoryContext(session, CoreFailureReason.INVENTORY_LIST_FAILED)) {
            is InventoryContextResult.Failure -> return InventoryDirectoryListResult.Failure(built.failure)
            is InventoryContextResult.Success -> built.context
        }
        return when (val result = inventorySource.listDirectory(context.identity, context.roots, context.capabilityUrl, query)) {
            is InventoryRuntimeDirectoryListResult.Failed ->
                InventoryDirectoryListResult.Failure(CoreFailure(CoreFailureReason.INVENTORY_LIST_FAILED, result.message))
            is InventoryRuntimeDirectoryListResult.Success -> InventoryDirectoryListResult.Success(
                InventoryDirectoryListing(
                    folders = mapFolders(result.folders),
                    items = mapDescriptors(result.items, query),
                ),
            )
        }
    }

    private fun inventoryContext(
        session: GemSession,
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

    private fun mapFolders(
        folders: List<LibomvInventoryFolderSnapshot>,
    ): List<InventoryFolderDescriptor> = folders
        .mapNotNull(LibomvInventoryFolderMapping::descriptor)
        .distinctBy { it.folderId }
        .sortedWith(compareBy<InventoryFolderDescriptor> { it.displayName.value.lowercase() }.thenBy { it.folderId.value })

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

    fun listDirectory(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
        query: InventoryItemQuery,
    ): InventoryRuntimeDirectoryListResult

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

            override fun listDirectory(
                identity: LibomvSessionIdentity,
                roots: LoginInventoryRoots,
                capabilityUrl: CapabilityUrl,
                query: InventoryItemQuery,
            ): InventoryRuntimeDirectoryListResult = InventoryRuntimeDirectoryListResult.Failed("inventory runtime unavailable")
        }
    }
}

internal sealed interface InventoryRuntimeResult {
    data class Success(val snapshot: LibomvAttachmentSnapshot) : InventoryRuntimeResult
    data class Failed(val reason: CoreFailureReason, val message: String) : InventoryRuntimeResult
}

internal sealed interface InventoryRuntimeDirectoryListResult {
    data class Success(
        val folders: List<LibomvInventoryFolderSnapshot>,
        val items: List<LibomvInventoryItemSnapshot>,
    ) : InventoryRuntimeDirectoryListResult

    data class Failed(val message: String) : InventoryRuntimeDirectoryListResult
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
