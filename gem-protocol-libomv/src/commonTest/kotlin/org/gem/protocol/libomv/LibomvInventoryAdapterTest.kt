package org.gem.protocol.libomv

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.SessionId
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.gem.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import org.gem.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.gem.protocol.libomv.runtime.InventoryRuntimeDirectoryListResult
import org.gem.protocol.libomv.runtime.InventoryRuntimeResult
import org.gem.protocol.libomv.runtime.InventoryRuntimeSource
import org.gem.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.gem.protocol.libomv.transport.CapabilityName
import org.gem.protocol.libomv.transport.CapabilityUrl
import org.gem.protocol.libomv.transport.CapabilityUrlProvider
import org.gem.protocol.libomv.transport.CapabilityUrlResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvInventoryAdapterTest {
    @Test
    fun `existing attachment routes through protocol runtime`() {
        val session = gemSession()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
        val adapter = adapter(
            session = session,
            source = source(
                existing = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK)),
            ),
        )

        val result = adapter.resolveExistingAttachment(session, request)

        assertEquals("landmark-item", assertIs<AttachmentResolutionResult.Resolved>(result).attachment.attachmentId.value)
    }

    @Test
    fun `existing attachment source failure maps through adapter`() {
        val session = gemSession()
        val adapter = adapter(
            session = session,
            source = source(
                existing = InventoryRuntimeResult.Failed(
                    CoreFailureReason.ATTACHMENT_NOT_FOUND,
                    "attachment unavailable",
                ),
            ),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(
            adapter.resolveExistingAttachment(
                session,
                ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("texture-item")),
            ),
        ).failure

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory adapter fallback still fails closed without runtime`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(gemSession()))

        val failure = assertIs<AttachmentResolutionResult.Failed>(
            adapter.resolveExistingAttachment(
                gemSession(),
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
            ),
        ).failure

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory list routes through protocol runtime`() {
        val session = gemSession()
        val adapter = adapter(
            session = session,
            source = source(
                listing = InventoryRuntimeDirectoryListResult.Success(
                    folders = emptyList(),
                    items = listOf(inventorySnapshot("landmark-item", "Venue Landmark", inventoryType = 3)),
                ),
            ),
        )

        val result = assertIs<InventoryItemListResult.Success>(
            adapter.listItems(session, InventoryItemQuery()),
        )

        assertEquals("Venue Landmark", result.items.single().displayName.value)
    }

    @Test
    fun `inventory directory routes through protocol runtime`() {
        val session = gemSession()
        val adapter = adapter(
            session = session,
            source = source(
                listing = InventoryRuntimeDirectoryListResult.Success(
                    folders = listOf(folderSnapshot("landmarks", ROOT_FOLDER_ID, "Landmarks")),
                    items = listOf(inventorySnapshot("landmark-item", "Venue Landmark", inventoryType = 3)),
                ),
            ),
        )

        val result = assertIs<InventoryDirectoryListResult.Success>(
            adapter.listDirectory(session, InventoryItemQuery()),
        )

        assertEquals(listOf("Landmarks"), result.listing.folders.map { it.displayName.value })
        assertEquals(listOf("Venue Landmark"), result.listing.items.map { it.displayName.value })
    }

    @Test
    fun `inventory list fallback still fails closed without runtime`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(gemSession()))

        val failure = assertIs<InventoryItemListResult.Failure>(
            adapter.listItems(gemSession(), InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory directory fallback still fails closed without runtime`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(gemSession()))

        val failure = assertIs<InventoryDirectoryListResult.Failure>(
            adapter.listDirectory(gemSession(), InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    private fun adapter(
        session: GemSession,
        source: InventoryRuntimeSource,
    ): LibomvInventoryAdapter {
        val clientSession = activeClientSession(session)
        return LibomvInventoryAdapter(
            clientSession = clientSession,
            inventoryRuntime = ProtocolInventoryRuntime(
                clientSession = clientSession,
                capabilityUrlProvider = readyCapabilityProvider(),
                inventorySource = source,
            ),
        )
    }

    private fun source(
        existing: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_NOT_FOUND,
            "attachment unavailable",
        ),
        listing: InventoryRuntimeDirectoryListResult = InventoryRuntimeDirectoryListResult.Failed("inventory unavailable"),
    ): InventoryRuntimeSource = object : InventoryRuntimeSource {
        override fun resolveExistingAttachment(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult = existing

        override fun listDirectory(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            query: InventoryItemQuery,
        ): InventoryRuntimeDirectoryListResult = listing
    }

    private fun snapshot(
        itemId: String,
        ownerId: String,
        kind: AttachmentKind,
    ): LibomvAttachmentSnapshot = LibomvAttachmentSnapshot(itemId, ownerId, kind)

    private fun inventorySnapshot(
        itemId: String,
        name: String,
        inventoryType: Int,
    ): LibomvInventoryItemSnapshot = LibomvInventoryItemSnapshot(
        itemId = itemId,
        ownerId = AGENT_ID,
        parentFolderId = ROOT_FOLDER_ID,
        assetId = "asset-$itemId",
        name = name,
        inventoryType = inventoryType,
    )

    private fun folderSnapshot(
        folderId: String,
        parentFolderId: String?,
        name: String,
    ): LibomvInventoryFolderSnapshot = LibomvInventoryFolderSnapshot(
        folderId = folderId,
        parentFolderId = parentFolderId,
        name = name,
    )

    private fun activeClientSession(session: GemSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
        inventoryRoots = LoginInventoryRoots(
            inventoryRootId = ROOT_FOLDER_ID,
            inventorySkeleton = emptyList(),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        ),
    )

    private fun readyCapabilityProvider(): CapabilityUrlProvider = CapabilityUrlProvider { _, name ->
        if (name == CapabilityName.FETCH_INVENTORY_DESCENDENTS2) {
            CapabilityUrlResult.Ready(CapabilityUrl("https://caps.example/inventory"))
        } else {
            CapabilityUrlResult.MappingGap("unexpected capability")
        }
    }

    private fun gemSession(): GemSession = GemSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val ROOT_FOLDER_ID = "22222222-2222-2222-2222-222222222222"
    }
}
