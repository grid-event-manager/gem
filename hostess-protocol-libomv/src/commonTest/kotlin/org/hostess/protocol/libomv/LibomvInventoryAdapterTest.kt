package org.hostess.protocol.libomv

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.runtime.InventoryRuntimeItemListResult
import org.hostess.protocol.libomv.runtime.InventoryRuntimeResult
import org.hostess.protocol.libomv.runtime.InventoryRuntimeSource
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvInventoryAdapterTest {
    @Test
    fun `existing attachment routes through protocol runtime`() {
        val session = hostessSession()
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
        val session = hostessSession()
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
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(hostessSession()))

        val failure = assertIs<AttachmentResolutionResult.Failed>(
            adapter.resolveExistingAttachment(
                hostessSession(),
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
            ),
        ).failure

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory list routes through protocol runtime`() {
        val session = hostessSession()
        val adapter = adapter(
            session = session,
            source = source(
                list = InventoryRuntimeItemListResult.Success(
                    listOf(inventorySnapshot("landmark-item", "Venue Landmark", inventoryType = 3)),
                ),
            ),
        )

        val result = assertIs<InventoryItemListResult.Success>(
            adapter.listItems(session, InventoryItemQuery()),
        )

        assertEquals("Venue Landmark", result.items.single().displayName.value)
    }

    @Test
    fun `inventory list fallback still fails closed without runtime`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(hostessSession()))

        val failure = assertIs<InventoryItemListResult.Failure>(
            adapter.listItems(hostessSession(), InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    private fun adapter(
        session: HostessSession,
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
        list: InventoryRuntimeItemListResult = InventoryRuntimeItemListResult.Failed("inventory unavailable"),
    ): InventoryRuntimeSource = object : InventoryRuntimeSource {
        override fun resolveExistingAttachment(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult = existing

        override fun listItems(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            capabilityUrl: CapabilityUrl,
            query: InventoryItemQuery,
        ): InventoryRuntimeItemListResult = list
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

    private fun activeClientSession(session: HostessSession): LibomvClientSession = LibomvClientSession.active(
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

    private fun hostessSession(): HostessSession = HostessSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val ROOT_FOLDER_ID = "22222222-2222-2222-2222-222222222222"
    }
}
