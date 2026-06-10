package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.services.SafeDiagnosticRedaction
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.llsd.LlsdValue
import org.hostess.protocol.libomv.llsd.LlsdXml
import org.hostess.protocol.libomv.llsd.asLong
import org.hostess.protocol.libomv.llsd.asString
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse

internal class ProtocolInventoryHttpSource(
    private val httpClient: ProtocolHttpClient,
) : InventoryRuntimeSource {
    override fun resolveExistingAttachment(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
        request: ExistingInventoryAttachment,
    ): InventoryRuntimeResult {
        val inventoryType = inventoryType(request.kind)
            ?: return InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_NOT_FOUND,
                "inventory attachment kind unsupported",
            )
        val items = when (val listed = listSnapshots(identity, roots, capabilityUrl)) {
            is InventorySnapshotListResult.Failed ->
                return InventoryRuntimeResult.Failed(CoreFailureReason.ATTACHMENT_NOT_FOUND, listed.message)
            is InventorySnapshotListResult.Success -> listed.items
        }
        val item = items.firstOrNull { it.itemId == request.itemId.value && it.inventoryType == inventoryType }
            ?: return InventoryRuntimeResult.Failed(CoreFailureReason.ATTACHMENT_NOT_FOUND, "attachment unavailable")
        return InventoryRuntimeResult.Success(
            LibomvAttachmentSnapshot(
                itemId = item.itemId,
                ownerId = item.ownerId,
                kind = request.kind,
            ),
        )
    }

    override fun listDirectory(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
        query: InventoryItemQuery,
    ): InventoryRuntimeDirectoryListResult =
        when (val listed = listSnapshots(identity, roots, capabilityUrl)) {
            is InventorySnapshotListResult.Failed -> InventoryRuntimeDirectoryListResult.Failed(listed.message)
            is InventorySnapshotListResult.Success -> InventoryRuntimeDirectoryListResult.Success(listed.folders, listed.items)
        }

    private fun listSnapshots(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        capabilityUrl: CapabilityUrl,
    ): InventorySnapshotListResult {
        val rootId = roots.inventoryRootId?.takeIf(String::isNotBlank)
            ?: return InventorySnapshotListResult.Failed("inventory root unavailable")
        val pending = ArrayDeque<FolderRequest>()
        val visited = mutableSetOf<String>()
        val folders = mutableListOf<LibomvInventoryFolderSnapshot>()
        val items = mutableListOf<LibomvInventoryItemSnapshot>()
        pending += FolderRequest(rootId, identity.agentId)

        while (pending.isNotEmpty()) {
            if (visited.size >= FOLDER_CAP) {
                return InventorySnapshotListResult.Failed("inventory folder cap exceeded")
            }
            val folder = pending.removeFirst()
            if (!visited.add(folder.folderId)) {
                continue
            }
            val response = when (val result = fetchFolder(capabilityUrl, folder)) {
                is InventoryHttpResult.Failed -> return InventorySnapshotListResult.Failed(result.message)
                is InventoryHttpResult.Success -> result.response
            }
            val folderResponses = responseFolders(response)
                ?: return InventorySnapshotListResult.Failed("inventory response invalid")
            for (folderResponse in folderResponses) {
                items += itemSnapshots(folderResponse)
                val childFolders = childFolders(
                    folderResponse = folderResponse,
                    fallbackOwnerId = folder.ownerId,
                    fallbackParentFolderId = folder.folderId,
                )
                folders += childFolders.map { it.snapshot }
                pending += childFolders
                    .map { it.request }
                    .filterNot { it.folderId in visited }
            }
        }

        return InventorySnapshotListResult.Success(folders, items)
    }

    private fun fetchFolder(
        capabilityUrl: CapabilityUrl,
        folder: FolderRequest,
    ): InventoryHttpResult = try {
        val response = httpClient.execute(fetchRequest(capabilityUrl, folder))
        if (response.statusCode !in 200..299) {
            InventoryHttpResult.Failed(
                "inventory transport unavailable: http_status=${response.statusCode}; ${responseDiagnostic(response)}",
            )
        } else {
            InventoryHttpResult.Success(response)
        }
    } catch (ex: ProtocolHttpException) {
        InventoryHttpResult.Failed(
            "inventory transport unavailable: ${SafeDiagnosticRedaction.redact(ex.message ?: "protocol http request failed")}",
        )
    }

    private fun fetchRequest(
        capabilityUrl: CapabilityUrl,
        folder: FolderRequest,
    ): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = capabilityUrl.value,
        headers = mapOf("Content-Type" to LLSD_XML),
        body = ProtocolHttpBody.TextBody(fetchBody(folder), LLSD_XML),
    )

    private fun fetchBody(folder: FolderRequest): String = buildString {
        append("<llsd><map><key>folders</key><array><map>")
        append("<key>folder_id</key><uuid>").append(xmlText(folder.folderId)).append("</uuid>")
        append("<key>owner_id</key><uuid>").append(xmlText(folder.ownerId)).append("</uuid>")
        append("<key>fetch_folders</key><boolean>true</boolean>")
        append("<key>fetch_items</key><boolean>true</boolean>")
        append("<key>sort_order</key><integer>0</integer>")
        append("</map></array></map></llsd>")
    }

    private fun responseFolders(response: ProtocolHttpResponse): List<Map<String, LlsdValue>>? {
        val fields = LlsdXml.parseMap(response.body) ?: return null
        val folders = fields[FOLDERS] as? LlsdValue.ArrayValue ?: return null
        return folders.values.mapNotNull { (it as? LlsdValue.MapValue)?.values }
    }

    private fun itemSnapshots(folderResponse: Map<String, LlsdValue>): List<LibomvInventoryItemSnapshot> {
        val items = folderResponse[ITEMS] as? LlsdValue.ArrayValue ?: return emptyList()
        val folderOwnerId = ownerId(folderResponse)
        return items.values.mapNotNull { value ->
            val fields = (value as? LlsdValue.MapValue)?.values ?: return@mapNotNull null
            LibomvInventoryItemSnapshot(
                itemId = fields[ITEM_ID]?.asString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                ownerId = itemOwnerId(fields, folderOwnerId) ?: return@mapNotNull null,
                parentFolderId = fields[PARENT_ID]?.asString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                assetId = fields[ASSET_ID]?.asString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                name = fields[NAME]?.asString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                inventoryType = fields[INV_TYPE]?.asInt() ?: return@mapNotNull null,
                permissions = fields[PERMISSIONS],
            )
        }
    }

    private fun childFolders(
        folderResponse: Map<String, LlsdValue>,
        fallbackOwnerId: String,
        fallbackParentFolderId: String,
    ): List<ChildFolder> {
        val folders = folderResponse[CATEGORIES] as? LlsdValue.ArrayValue ?: return emptyList()
        return folders.values.mapNotNull { value ->
            val fields = (value as? LlsdValue.MapValue)?.values ?: return@mapNotNull null
            val folderId = fields[CATEGORY_ID]?.asString()?.takeIf(String::isNotBlank)
                ?: fields[FOLDER_ID]?.asString()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val ownerId = ownerId(fields) ?: fallbackOwnerId
            ChildFolder(
                snapshot = LibomvInventoryFolderSnapshot(
                    folderId = folderId,
                    parentFolderId = fields[PARENT_ID]?.asString()?.takeIf(String::isNotBlank)
                        ?: fallbackParentFolderId,
                    name = fields[NAME]?.asString()?.takeIf(String::isNotBlank) ?: folderId,
                ),
                request = FolderRequest(
                    folderId = folderId,
                    ownerId = ownerId,
                ),
            )
        }
    }

    private fun ownerId(fields: Map<String, LlsdValue>): String? =
        fields[OWNER_ID]?.asString()?.takeIf(String::isNotBlank)
            ?: fields[AGENT_ID]?.asString()?.takeIf(String::isNotBlank)

    private fun itemOwnerId(fields: Map<String, LlsdValue>, folderOwnerId: String?): String? =
        ownerId(fields)
            ?: permissionOwnerId(fields)
            ?: folderOwnerId

    private fun permissionOwnerId(fields: Map<String, LlsdValue>): String? {
        val permissions = (fields[PERMISSIONS] as? LlsdValue.MapValue)?.values ?: return null
        return ownerId(permissions)
    }

    private fun LlsdValue.asInt(): Int? = asLong()
        ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
        ?.toInt()

    private fun responseDiagnostic(response: ProtocolHttpResponse): String =
        SafeDiagnosticRedaction.redact("${response.redactedSummary}; ${bodyDiagnostic(response.body)}")

    private fun bodyDiagnostic(body: ByteArray): String =
        SafeDiagnosticRedaction.excerpt(body.decodeToString())
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"

    private fun xmlText(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private data class FolderRequest(
        val folderId: String,
        val ownerId: String,
    )

    private data class ChildFolder(
        val snapshot: LibomvInventoryFolderSnapshot,
        val request: FolderRequest,
    )

    private sealed interface InventoryHttpResult {
        data class Success(val response: ProtocolHttpResponse) : InventoryHttpResult
        data class Failed(val message: String) : InventoryHttpResult
    }

    private sealed interface InventorySnapshotListResult {
        data class Success(
            val folders: List<LibomvInventoryFolderSnapshot>,
            val items: List<LibomvInventoryItemSnapshot>,
        ) : InventorySnapshotListResult

        data class Failed(val message: String) : InventorySnapshotListResult
    }

    private fun inventoryType(kind: AttachmentKind): Int? =
        when (kind) {
            AttachmentKind.LANDMARK -> LANDMARK_INVENTORY_TYPE
            AttachmentKind.TEXTURE -> TEXTURE_INVENTORY_TYPE
        }

    private companion object {
        const val LLSD_XML = "application/llsd+xml"
        const val FOLDER_CAP = 200
        const val TEXTURE_INVENTORY_TYPE = 0
        const val LANDMARK_INVENTORY_TYPE = 3

        const val FOLDERS = "folders"
        const val CATEGORIES = "categories"
        const val ITEMS = "items"
        const val FOLDER_ID = "folder_id"
        const val CATEGORY_ID = "category_id"
        const val OWNER_ID = "owner_id"
        const val AGENT_ID = "agent_id"
        const val ITEM_ID = "item_id"
        const val PARENT_ID = "parent_id"
        const val ASSET_ID = "asset_id"
        const val NAME = "name"
        const val INV_TYPE = "inv_type"
        const val PERMISSIONS = "permissions"
    }
}
