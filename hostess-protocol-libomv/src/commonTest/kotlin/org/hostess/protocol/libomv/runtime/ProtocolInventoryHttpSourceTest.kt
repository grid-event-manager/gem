package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolInventoryHttpSourceTest {
    @Test
    fun `request body matches fetch inventory descendents shape`() {
        val httpClient = RecordingHttpClient { emptyFolderResponse() }
        val source = ProtocolInventoryHttpSource(httpClient)

        source.listItems(identity(), roots(), capabilityUrl(), InventoryItemQuery())

        val request = httpClient.requests.single()
        assertEquals("POST", request.method)
        assertEquals(capabilityUrl().value, request.url)
        assertEquals("application/llsd+xml", request.headers["Content-Type"])
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body).content
        assertContains(body, "<key>folders</key><array><map>")
        assertContains(body, "<key>folder_id</key><uuid>$ROOT_FOLDER_ID</uuid>")
        assertContains(body, "<key>owner_id</key><uuid>$AGENT_ID</uuid>")
        assertContains(body, "<key>fetch_folders</key><boolean>true</boolean>")
        assertContains(body, "<key>fetch_items</key><boolean>true</boolean>")
        assertContains(body, "<key>sort_order</key><integer>0</integer>")
    }

    @Test
    fun `recurses folders and returns item snapshots`() {
        val httpClient = RecordingHttpClient { requestNumber ->
            if (requestNumber == 1) {
                folderResponse(
                    categories = listOf(category(CHILD_FOLDER_ID, ROOT_FOLDER_ID)),
                    items = listOf(item("root-landmark", "Root Landmark", ROOT_FOLDER_ID, inventoryType = 3)),
                )
            } else {
                folderResponse(
                    items = listOf(item("child-notecard", "Child Notes", CHILD_FOLDER_ID, inventoryType = 7)),
                )
            }
        }
        val source = ProtocolInventoryHttpSource(httpClient)

        val result = assertIs<InventoryRuntimeItemListResult.Success>(
            source.listItems(identity(), roots(), capabilityUrl(), InventoryItemQuery()),
        )

        assertEquals(listOf("root-landmark", "child-notecard"), result.items.map { it.itemId })
        assertEquals(2, httpClient.requests.size)
        assertContains(assertIs<ProtocolHttpBody.TextBody>(httpClient.requests[1].body).content, CHILD_FOLDER_ID)
    }

    @Test
    fun `maps OpenSim caps item owner from permissions`() {
        val source = ProtocolInventoryHttpSource(
            RecordingHttpClient {
                folderResponse(
                    items = listOf(openSimItemWithoutItemOwner("landmark-item", "Venue Landmark", ROOT_FOLDER_ID)),
                )
            },
        )

        val result = assertIs<InventoryRuntimeItemListResult.Success>(
            source.listItems(identity(), roots(), capabilityUrl(), InventoryItemQuery()),
        )

        assertEquals("landmark-item", result.items.single().itemId)
        assertEquals(AGENT_ID, result.items.single().ownerId)
    }

    @Test
    fun `malformed top level response fails`() {
        val source = ProtocolInventoryHttpSource(
            RecordingHttpClient { response("<llsd><map><key>not_folders</key><array /></map></llsd>") },
        )

        val result = assertIs<InventoryRuntimeItemListResult.Failed>(
            source.listItems(identity(), roots(), capabilityUrl(), InventoryItemQuery()),
        )

        assertEquals("inventory response invalid", result.message)
    }

    @Test
    fun `folder cap breach fails`() {
        val source = ProtocolInventoryHttpSource(
            RecordingHttpClient { requestNumber ->
                val childId = "folder-${requestNumber.toString().padStart(3, '0')}"
                folderResponse(categories = listOf(category(childId, ROOT_FOLDER_ID)))
            },
        )

        val result = assertIs<InventoryRuntimeItemListResult.Failed>(
            source.listItems(identity(), roots(), capabilityUrl(), InventoryItemQuery()),
        )

        assertEquals("inventory folder cap exceeded", result.message)
    }

    @Test
    fun `resolves landmark attachment by catalogue item id and rejects texture`() {
        val source = ProtocolInventoryHttpSource(
            RecordingHttpClient {
                folderResponse(
                    items = listOf(item("landmark-item", "Venue Landmark", ROOT_FOLDER_ID, inventoryType = 3)),
                )
            },
        )

        val resolved = assertIs<InventoryRuntimeResult.Success>(
            source.resolveExistingAttachment(
                identity(),
                roots(),
                capabilityUrl(),
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
            ),
        )
        val rejected = assertIs<InventoryRuntimeResult.Failed>(
            source.resolveExistingAttachment(
                identity(),
                roots(),
                capabilityUrl(),
                ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("texture-item")),
            ),
        )

        assertEquals("landmark-item", resolved.snapshot.itemId)
        assertEquals(AttachmentKind.LANDMARK, resolved.snapshot.kind)
        assertEquals("inventory attachment kind unsupported", rejected.message)
    }

    private class RecordingHttpClient(
        private val responseForRequest: (Int) -> ProtocolHttpResponse,
    ) : ProtocolHttpClient {
        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            return responseForRequest(requests.size)
        }
    }

    private fun emptyFolderResponse(): ProtocolHttpResponse = folderResponse()

    private fun folderResponse(
        categories: List<String> = emptyList(),
        items: List<String> = emptyList(),
    ): ProtocolHttpResponse = response(
        buildString {
            append("<llsd><map><key>folders</key><array><map>")
            append("<key>categories</key><array>")
            categories.forEach(::append)
            append("</array><key>items</key><array>")
            items.forEach(::append)
            append("</array></map></array></map></llsd>")
        },
    )

    private fun category(
        folderId: String,
        parentId: String,
    ): String = buildString {
        append("<map>")
        append("<key>category_id</key><uuid>").append(folderId).append("</uuid>")
        append("<key>parent_id</key><uuid>").append(parentId).append("</uuid>")
        append("<key>agent_id</key><uuid>").append(AGENT_ID).append("</uuid>")
        append("<key>name</key><string>Folder</string>")
        append("</map>")
    }

    private fun openSimItemWithoutItemOwner(
        itemId: String,
        name: String,
        parentId: String,
    ): String = buildString {
        append("<map>")
        append("<key>item_id</key><uuid>").append(itemId).append("</uuid>")
        append("<key>parent_id</key><uuid>").append(parentId).append("</uuid>")
        append("<key>asset_id</key><uuid>asset-").append(itemId).append("</uuid>")
        append("<key>permissions</key><map>")
        append("<key>owner_id</key><uuid>").append(AGENT_ID).append("</uuid>")
        append("<key>owner_mask</key><integer>581632</integer>")
        append("</map>")
        append("<key>name</key><string>").append(name).append("</string>")
        append("<key>inv_type</key><integer>3</integer>")
        append("</map>")
    }

    private fun item(
        itemId: String,
        name: String,
        parentId: String,
        inventoryType: Int,
    ): String = buildString {
        append("<map>")
        append("<key>item_id</key><uuid>").append(itemId).append("</uuid>")
        append("<key>agent_id</key><uuid>").append(AGENT_ID).append("</uuid>")
        append("<key>parent_id</key><uuid>").append(parentId).append("</uuid>")
        append("<key>asset_id</key><uuid>asset-").append(itemId).append("</uuid>")
        append("<key>name</key><string>").append(name).append("</string>")
        append("<key>inv_type</key><integer>").append(inventoryType).append("</integer>")
        append("</map>")
    }

    private fun response(body: String): ProtocolHttpResponse = ProtocolHttpResponse(
        statusCode = 200,
        headers = emptyMap(),
        body = body.encodeToByteArray(),
        redactedSummary = "POST <redacted> -> 200",
    )

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = "session",
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )

    private fun roots(): LoginInventoryRoots = LoginInventoryRoots(
        inventoryRootId = ROOT_FOLDER_ID,
        inventorySkeleton = emptyList(),
        libraryRootId = null,
        libraryOwnerId = null,
        librarySkeleton = emptyList(),
    )

    private fun capabilityUrl(): CapabilityUrl = CapabilityUrl("https://caps.example/inventory")

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val ROOT_FOLDER_ID = "22222222-2222-2222-2222-222222222222"
        const val CHILD_FOLDER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
