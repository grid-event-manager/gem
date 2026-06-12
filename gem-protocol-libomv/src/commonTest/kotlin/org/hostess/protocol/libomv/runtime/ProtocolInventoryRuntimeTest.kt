package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryDirectoryListResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ProtocolInventoryRuntimeTest {
    @Test
    fun `existing item resolves when kind matches`() {
        val session = hostessSession()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK))
        }

        val result = runtime(session, source).resolveExistingAttachment(session, request)

        val attachment = assertIs<AttachmentResolutionResult.Resolved>(result).attachment
        assertEquals("landmark-item", attachment.attachmentId.value)
        assertEquals("owner", attachment.ownerId.value)
        assertEquals(AttachmentKind.LANDMARK, attachment.kind)
        assertEquals(listOf(request), source.existingRequests)
    }

    @Test
    fun `existing item not found maps to attachment not found`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_NOT_FOUND,
                "attachment unavailable",
            )
        }

        val result = runtime(session, source).resolveExistingAttachment(
            session,
            ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("missing-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment unavailable", failure.redactedMessage)
    }

    @Test
    fun `existing item kind mismatch fails without leaking item ID`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Success(snapshot("texture-item", "owner", AttachmentKind.TEXTURE))
        }

        val result = runtime(session, source).resolveExistingAttachment(
            session,
            ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("texture-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment kind mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("texture-item"))
    }

    @Test
    fun `attachment runtime rejects mismatched session without calling source`() {
        val runtime = ProtocolInventoryRuntime(
            clientSession = activeClientSession(hostessSession("live-session")),
            capabilityUrlProvider = readyCapabilityProvider(),
            inventorySource = FakeInventoryRuntimeSource(),
        )

        val result = runtime.resolveExistingAttachment(
            hostessSession("other-session"),
            ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("hostess session mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure.redactedMessage.orEmpty().contains("other-session"))
    }

    @Test
    fun `inventory list maps descriptors and filters query kinds`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            listResult = InventoryRuntimeDirectoryListResult.Success(
                folders = listOf(folderSnapshot("landmarks", ROOT_FOLDER_ID, "Landmarks")),
                items = listOf(
                    inventorySnapshot("z-landmark", "Zoo Landmark", inventoryType = 3),
                    inventorySnapshot("texture", "Event Poster", inventoryType = 0),
                    inventorySnapshot("a-landmark", "Alpha Landmark", inventoryType = 3),
                    inventorySnapshot("notecard", "Venue Notes", inventoryType = 7),
                    inventorySnapshot("object", "Object", inventoryType = 6),
                ),
            )
        }

        val result = assertIs<InventoryItemListResult.Success>(
            runtime(session, source).listItems(
                session,
                InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK)),
            ),
        )

        assertEquals(listOf("Alpha Landmark", "Zoo Landmark"), result.items.map { it.displayName.value })
        assertEquals(listOf(InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK))), source.listRequests)
    }

    @Test
    fun `inventory directory maps folders and texture descriptors`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            listResult = InventoryRuntimeDirectoryListResult.Success(
                folders = listOf(
                    folderSnapshot("textures", ROOT_FOLDER_ID, "Textures"),
                    folderSnapshot("landmarks", ROOT_FOLDER_ID, "Landmarks"),
                ),
                items = listOf(
                    inventorySnapshot("z-texture", "Zoo Poster", inventoryType = 0),
                    inventorySnapshot("a-texture", "Alpha Poster", inventoryType = 0),
                    inventorySnapshot("landmark", "Venue Landmark", inventoryType = 3),
                ),
            )
        }

        val result = assertIs<InventoryDirectoryListResult.Success>(
            runtime(session, source).listDirectory(
                session,
                InventoryItemQuery(kinds = setOf(InventoryItemKind.TEXTURE)),
            ),
        )

        assertEquals(listOf("Landmarks", "Textures"), result.listing.folders.map { it.displayName.value })
        assertEquals(listOf("Alpha Poster", "Zoo Poster"), result.listing.items.map { it.displayName.value })
        assertEquals(listOf(InventoryItemKind.TEXTURE, InventoryItemKind.TEXTURE), result.listing.items.map { it.kind })
    }

    @Test
    fun `inventory list inactive session returns inventory list failure without source call`() {
        val runtime = ProtocolInventoryRuntime(
            clientSession = LibomvClientSession.inactive(),
            capabilityUrlProvider = readyCapabilityProvider(),
            inventorySource = FakeInventoryRuntimeSource(),
        )

        val failure = assertIs<InventoryItemListResult.Failure>(
            runtime.listItems(hostessSession(), InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("protocol session inactive", failure.redactedMessage)
    }

    @Test
    fun `inventory list missing roots returns inventory list failure`() {
        val session = hostessSession()
        val runtime = ProtocolInventoryRuntime(
            clientSession = activeClientSession(session, inventoryRoots = LoginInventoryRoots.empty()),
            capabilityUrlProvider = readyCapabilityProvider(),
            inventorySource = FakeInventoryRuntimeSource(),
        )

        val failure = assertIs<InventoryItemListResult.Failure>(
            runtime.listItems(session, InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("inventory root unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory list missing capability returns inventory list failure`() {
        val session = hostessSession()
        val runtime = ProtocolInventoryRuntime(
            clientSession = activeClientSession(session),
            capabilityUrlProvider = CapabilityUrlProvider { _, _ -> CapabilityUrlResult.TransportGap("inventory cap unavailable") },
            inventorySource = FakeInventoryRuntimeSource(),
        )

        val failure = assertIs<InventoryItemListResult.Failure>(
            runtime.listItems(session, InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("inventory cap unavailable", failure.redactedMessage)
    }

    private fun runtime(
        session: HostessSession,
        source: FakeInventoryRuntimeSource,
    ): ProtocolInventoryRuntime = ProtocolInventoryRuntime(
        clientSession = activeClientSession(session),
        capabilityUrlProvider = readyCapabilityProvider(),
        inventorySource = source,
    )

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

    private fun activeClientSession(
        session: HostessSession,
        inventoryRoots: LoginInventoryRoots = LoginInventoryRoots(
            inventoryRootId = ROOT_FOLDER_ID,
            inventorySkeleton = emptyList(),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        ),
    ): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
        inventoryRoots = inventoryRoots,
    )

    private fun readyCapabilityProvider(): CapabilityUrlProvider = CapabilityUrlProvider { _, name ->
        if (name == CapabilityName.FETCH_INVENTORY_DESCENDENTS2) {
            CapabilityUrlResult.Ready(CapabilityUrl("https://caps.example/inventory"))
        } else {
            CapabilityUrlResult.MappingGap("unexpected capability")
        }
    }

    private fun hostessSession(id: String = "live-session"): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private class FakeInventoryRuntimeSource : InventoryRuntimeSource {
        var existingResult: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_NOT_FOUND,
            "attachment unavailable",
        )
        var listResult: InventoryRuntimeDirectoryListResult =
            InventoryRuntimeDirectoryListResult.Failed("inventory unavailable")
        val existingRequests = mutableListOf<ExistingInventoryAttachment>()
        val listRequests = mutableListOf<InventoryItemQuery>()

        override fun resolveExistingAttachment(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult {
            existingRequests += request
            return existingResult
        }

        override fun listDirectory(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            query: InventoryItemQuery,
        ): InventoryRuntimeDirectoryListResult {
            listRequests += query
            return listResult
        }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val ROOT_FOLDER_ID = "22222222-2222-2222-2222-222222222222"
    }
}
