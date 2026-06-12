package org.gem.protocol.libomv.mapping

import org.gem.core.domain.CoreFailure
import org.gem.protocol.libomv.llsd.LlsdValue
import org.gem.protocol.libomv.llsd.asLong
import org.gem.protocol.libomv.llsd.asString

internal data class LoginInventoryRoots(
    val inventoryRootId: String?,
    val inventorySkeleton: List<LoginInventoryFolder>,
    val libraryRootId: String?,
    val libraryOwnerId: String?,
    val librarySkeleton: List<LoginInventoryFolder>,
) {
    companion object {
        fun empty(): LoginInventoryRoots = LoginInventoryRoots(
            inventoryRootId = null,
            inventorySkeleton = emptyList(),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        )

        fun fromLlsd(
            fields: Map<String, LlsdValue>,
            agentId: String?,
        ): LoginInventoryRoots {
            val libraryOwnerId = mappedId(fields[LoginKeys.INVENTORY_LIB_OWNER], LoginKeys.AGENT_ID)
            return LoginInventoryRoots(
                inventoryRootId = mappedId(fields[LoginKeys.INVENTORY_ROOT], LoginKeys.FOLDER_ID),
                inventorySkeleton = skeletonFolders(
                    value = fields[LoginKeys.INVENTORY_SKELETON],
                    defaultOwnerId = agentId,
                ),
                libraryRootId = mappedId(fields[LoginKeys.INVENTORY_LIB_ROOT], LoginKeys.FOLDER_ID),
                libraryOwnerId = libraryOwnerId,
                librarySkeleton = skeletonFolders(
                    value = fields[LoginKeys.INVENTORY_SKEL_LIB],
                    defaultOwnerId = libraryOwnerId,
                ),
            )
        }

        private fun mappedId(value: LlsdValue?, fieldName: String): String? {
            val array = value as? LlsdValue.ArrayValue ?: return null
            val rootMap = array.values.singleOrNull() as? LlsdValue.MapValue ?: return null
            return rootMap.values[fieldName]?.asString()?.takeIf(String::isNotBlank)
        }

        private fun skeletonFolders(
            value: LlsdValue?,
            defaultOwnerId: String?,
        ): List<LoginInventoryFolder> {
            val array = value as? LlsdValue.ArrayValue ?: return emptyList()
            return array.values.mapNotNull { entry ->
                val fields = (entry as? LlsdValue.MapValue)?.values ?: return@mapNotNull null
                val folderId = fields[LoginKeys.FOLDER_ID]?.asString()?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                LoginInventoryFolder(
                    folderId = folderId,
                    parentId = fields[LoginKeys.PARENT_ID]?.asString()?.takeIf(String::isNotBlank),
                    ownerId = defaultOwnerId?.takeIf(String::isNotBlank),
                    name = fields[LoginKeys.NAME]?.asString()?.takeIf(String::isNotBlank),
                    typeDefault = fields[LoginKeys.TYPE_DEFAULT]?.asInt(),
                    version = fields[LoginKeys.VERSION_FIELD]?.asInt(),
                )
            }
        }

        private fun LlsdValue.asInt(): Int? = asLong()
            ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
            ?.toInt()
    }
}

internal data class LoginInventoryFolder(
    val folderId: String,
    val parentId: String?,
    val ownerId: String?,
    val name: String?,
    val typeDefault: Int?,
    val version: Int?,
)

internal sealed interface LoginInventoryRootsResult {
    data class Success(val roots: LoginInventoryRoots) : LoginInventoryRootsResult
    data class Failure(val failure: CoreFailure) : LoginInventoryRootsResult
}
