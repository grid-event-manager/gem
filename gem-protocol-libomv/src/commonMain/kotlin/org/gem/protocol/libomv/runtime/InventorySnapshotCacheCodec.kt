package org.gem.protocol.libomv.runtime

import org.gem.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.gem.protocol.libomv.mapping.LibomvInventoryItemSnapshot

internal object InventorySnapshotCacheCodec {
    fun encode(snapshot: InventorySnapshot): String = buildString {
        appendLine(MAGIC)
        snapshot.folders.forEach { folder ->
            appendLine(
                listOf(
                    FOLDER,
                    encodeField(folder.folderId),
                    encodeNullable(folder.parentFolderId),
                    encodeField(folder.name),
                ).joinToString(SEPARATOR),
            )
        }
        snapshot.items.forEach { item ->
            appendLine(
                listOf(
                    ITEM,
                    encodeField(item.itemId),
                    encodeField(item.ownerId),
                    encodeField(item.parentFolderId),
                    encodeField(item.assetId),
                    encodeField(item.name),
                    item.inventoryType.toString(),
                    encodeCopyable(item.copyable),
                ).joinToString(SEPARATOR),
            )
        }
    }

    fun decode(raw: String): InventorySnapshot? {
        val lines = raw.lineSequence().filter(String::isNotEmpty).toList()
        if (lines.firstOrNull() != MAGIC) {
            return null
        }
        val folders = mutableListOf<LibomvInventoryFolderSnapshot>()
        val items = mutableListOf<LibomvInventoryItemSnapshot>()
        for (line in lines.drop(1)) {
            val fields = line.split(SEPARATOR)
            when (fields.firstOrNull()) {
                FOLDER -> {
                    if (fields.size != FOLDER_FIELD_COUNT) {
                        return null
                    }
                    folders += LibomvInventoryFolderSnapshot(
                        folderId = decodeField(fields[1]) ?: return null,
                        parentFolderId = if (fields[2] == NULL_FIELD) {
                            null
                        } else {
                            decodeField(fields[2]) ?: return null
                        },
                        name = decodeField(fields[3]) ?: return null,
                    )
                }
                ITEM -> {
                    if (fields.size != ITEM_FIELD_COUNT) {
                        return null
                    }
                    items += LibomvInventoryItemSnapshot(
                        itemId = decodeField(fields[1]) ?: return null,
                        ownerId = decodeField(fields[2]) ?: return null,
                        parentFolderId = decodeField(fields[3]) ?: return null,
                        assetId = decodeField(fields[4]) ?: return null,
                        name = decodeField(fields[5]) ?: return null,
                        inventoryType = fields[6].toIntOrNull() ?: return null,
                        copyable = when (fields[7]) {
                            TRUE_FIELD -> true
                            FALSE_FIELD -> false
                            UNKNOWN_FIELD -> null
                            else -> return null
                        },
                    )
                }
                else -> return null
            }
        }
        return InventorySnapshot(folders, items)
    }

    private fun encodeNullable(value: String?): String =
        value?.let(::encodeField) ?: NULL_FIELD

    private fun encodeCopyable(value: Boolean?): String =
        when (value) {
            true -> TRUE_FIELD
            false -> FALSE_FIELD
            null -> UNKNOWN_FIELD
        }

    private fun encodeField(value: String): String = buildString {
        value.forEach { char ->
            append(
                when (char) {
                    '%' -> "%25"
                    '\t' -> "%09"
                    '\n' -> "%0A"
                    '\r' -> "%0D"
                    else -> char.toString()
                },
            )
        }
    }

    private fun decodeField(value: String): String? = buildString {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char != '%') {
                append(char)
                index += 1
                continue
            }
            val code = value.substring(index, (index + ESCAPE_LENGTH).coerceAtMost(value.length))
            append(
                when (code) {
                    "%25" -> "%"
                    "%09" -> "\t"
                    "%0A" -> "\n"
                    "%0D" -> "\r"
                    else -> return null
                },
            )
            index += ESCAPE_LENGTH
        }
    }

    private const val MAGIC = "HOSTESS_INVENTORY_SNAPSHOT_CACHE_V1"
    private const val FOLDER = "folder"
    private const val ITEM = "item"
    private const val SEPARATOR = "\t"
    private const val NULL_FIELD = "-"
    private const val TRUE_FIELD = "1"
    private const val FALSE_FIELD = "0"
    private const val UNKNOWN_FIELD = "u"
    private const val FOLDER_FIELD_COUNT = 4
    private const val ITEM_FIELD_COUNT = 8
    private const val ESCAPE_LENGTH = 3
}
