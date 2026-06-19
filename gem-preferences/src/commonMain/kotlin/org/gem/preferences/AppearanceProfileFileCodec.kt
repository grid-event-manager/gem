package org.gem.preferences

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileCompletion
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot
import org.gem.core.appearance.AppearanceTextTarget

class AppearanceProfileFileCodec {
    fun encode(snapshot: AppearanceProfileStoreSnapshot): String =
        buildString {
            appendKeyValue(FORMAT_VERSION_KEY, FORMAT_VERSION)
            appendKeyValue(ACTIVE_LIGHT_KEY, snapshot.activeLightProfileId?.value.orEmpty().escaped())
            appendKeyValue(ACTIVE_DARK_KEY, snapshot.activeDarkProfileId?.value.orEmpty().escaped())
            snapshot.customProfiles
                .sortedWith(profileComparator)
                .forEachIndexed { index, profile ->
                    appendProfile(index, profile)
                }
        }

    fun decode(text: String): AppearanceProfileStoreLoadResult {
        if (text.isBlank()) {
            return invalid("empty_file")
        }

        val records = records(text)
        val values = mutableMapOf<String, String>()
        for (record in records) {
            val separator = record.indexOf(KEY_VALUE_SEPARATOR)
            if (separator < 1) {
                return invalid("malformed_line")
            }
            val key = record.substring(0, separator)
            val rawValue = record.substring(separator + 1)
            if (key in values) {
                return invalid("duplicate_key:$key")
            }
            values[key] = rawValue
        }

        if (records.getOrNull(0) != "$FORMAT_VERSION_KEY=$FORMAT_VERSION") {
            return invalid("invalid_format_version")
        }
        if (records.getOrNull(1)?.startsWith("$ACTIVE_LIGHT_KEY=") != true ||
            records.getOrNull(2)?.startsWith("$ACTIVE_DARK_KEY=") != true
        ) {
            return invalid("missing_active_profile")
        }

        val activeLight = values.getValue(ACTIVE_LIGHT_KEY).decodeValue()
            .getOrElse { return invalid(it) }
            .toProfileIdOrNull()
            .getOrElse { return invalid(it) }
        val activeDark = values.getValue(ACTIVE_DARK_KEY).decodeValue()
            .getOrElse { return invalid(it) }
            .toProfileIdOrNull()
            .getOrElse { return invalid(it) }
        val grouped = mutableMapOf<Int, MutableMap<String, String>>()

        for ((key, rawValue) in values) {
            when (key) {
                FORMAT_VERSION_KEY, ACTIVE_LIGHT_KEY, ACTIVE_DARK_KEY -> Unit
                else -> {
                    val profileKey = parseProfileKey(key).getOrElse { return invalid(it) }
                    grouped.getOrPut(profileKey.index) { mutableMapOf() }[profileKey.field] = rawValue
                }
            }
        }

        val indices = grouped.keys.sorted()
        if (indices != indices.indices.toList()) {
            return invalid("non_contiguous_profile_index")
        }

        val profiles = indices.map { index ->
            decodeProfile(index, grouped.getValue(index)).getOrElse { return invalid(it) }
        }
        if (profiles.map { it.id }.toSet().size != profiles.size) {
            return invalid("duplicate_profile_id")
        }

        return AppearanceProfileStoreLoadResult.Loaded(
            AppearanceProfileStoreSnapshot(
                customProfiles = profiles,
                activeLightProfileId = activeLight,
                activeDarkProfileId = activeDark,
            ),
        )
    }

    private fun StringBuilder.appendProfile(
        index: Int,
        profile: AppearanceProfile,
    ) {
        require(profile.source == AppearanceProfileSource.CUSTOM) {
            "Only custom Appearance profiles can be persisted."
        }
        val prefix = "profile.$index"
        appendKeyValue("$prefix.id", profile.id.value.escaped())
        appendKeyValue("$prefix.name", profile.name.value.escaped())
        appendKeyValue("$prefix.mode", profile.mode.name.escaped())
        for (target in AppearanceTextTarget.entries) {
            val targetPrefix = "$prefix.text.${target.storageKey}"
            appendKeyValue("$targetPrefix.font", profile.textFonts.getValue(target).value.escaped())
            appendKeyValue("$targetPrefix.color", profile.textColors.getValue(target).value.escaped())
        }
        for (target in AppearanceElementTarget.entries) {
            appendKeyValue(
                key = "$prefix.element.${target.storageKey}.color",
                value = profile.elementColors.getValue(target).value.escaped(),
            )
        }
    }

    private fun decodeProfile(
        index: Int,
        values: Map<String, String>,
    ): Result<AppearanceProfile> {
        val profilePrefix = "profile.$index"
        val id = requiredDecoded(values, "$profilePrefix.id").getOrElse { return Result.failure(it) }
            .toProfileId().getOrElse { return Result.failure(it) }
        val name = requiredDecoded(values, "$profilePrefix.name").getOrElse { return Result.failure(it) }
            .toProfileName().getOrElse { return Result.failure(it) }
        val mode = requiredDecoded(values, "$profilePrefix.mode").getOrElse { return Result.failure(it) }
            .toMode().getOrElse { return Result.failure(it) }
        val textFonts = mutableMapOf<AppearanceTextTarget, AppearanceFontFamily>()
        val textColors = mutableMapOf<AppearanceTextTarget, AppearanceColor>()
        val elementColors = mutableMapOf<AppearanceElementTarget, AppearanceColor>()
        val expectedFields = mutableSetOf(
            "$profilePrefix.id",
            "$profilePrefix.name",
            "$profilePrefix.mode",
        )

        for (target in AppearanceTextTarget.entries) {
            val targetPrefix = "$profilePrefix.text.${target.storageKey}"
            val fontKey = "$targetPrefix.font"
            val colorKey = "$targetPrefix.color"
            expectedFields += fontKey
            expectedFields += colorKey
            textFonts[target] = requiredDecoded(values, fontKey)
                .getOrElse { return Result.failure(it) }
                .toFontFamily()
                .getOrElse { return Result.failure(it) }
            textColors[target] = requiredDecoded(values, colorKey)
                .getOrElse { return Result.failure(it) }
                .toColor()
                .getOrElse { return Result.failure(it) }
        }
        for (target in AppearanceElementTarget.entries) {
            val key = "$profilePrefix.element.${target.storageKey}.color"
            expectedFields += key
            val decoded = values[key]?.decodeValue()
                ?.getOrElse { return Result.failure(it) }
                ?: run {
                    if (target in COMPATIBLE_MISSING_ELEMENT_TARGETS) {
                        null
                    } else {
                        return Result.failure(InvalidProfile("missing_key:$key"))
                    }
                }
            if (decoded != null) {
                elementColors[target] = decoded.toColor()
                    .getOrElse { return Result.failure(it) }
            }
        }
        val unexpected = values.keys - expectedFields
        if (unexpected.isNotEmpty()) {
            return Result.failure(InvalidProfile("malformed_key:${unexpected.first()}"))
        }

        return Result.success(
            AppearanceProfile(
                id = id,
                name = name,
                mode = mode,
                source = AppearanceProfileSource.CUSTOM,
                textFonts = textFonts,
                textColors = textColors,
                elementColors = AppearanceProfileCompletion.completeElementColors(mode, elementColors),
            ),
        )
    }

    private fun requiredDecoded(
        values: Map<String, String>,
        key: String,
    ): Result<String> {
        val raw = values[key] ?: return Result.failure(InvalidProfile("missing_key:$key"))
        return raw.decodeValue()
    }

    private fun parseProfileKey(key: String): Result<ProfileKey> {
        val parts = key.split(".")
        if (parts.size !in 3..5 || parts[0] != "profile") {
            return Result.failure(InvalidProfile("malformed_key:$key"))
        }
        val index = parts[1].toIntOrNull() ?: return Result.failure(InvalidProfile("malformed_key:$key"))
        if (index < 0) {
            return Result.failure(InvalidProfile("malformed_key:$key"))
        }
        return when {
            parts.size == 3 && parts[2] in TOP_LEVEL_PROFILE_FIELDS -> Result.success(ProfileKey(index, key))
            parts.size == 5 && parts[2] == "text" && parts[4] in TEXT_PROFILE_FIELDS -> {
                if (AppearanceTextTarget.entries.none { it.storageKey == parts[3] }) {
                    Result.failure(InvalidProfile("invalid_target:${parts[3]}"))
                } else {
                    Result.success(ProfileKey(index, key))
                }
            }
            parts.size == 5 && parts[2] == "element" && parts[4] == COLOR_FIELD -> {
                if (AppearanceElementTarget.entries.none { it.storageKey == parts[3] }) {
                    Result.failure(InvalidProfile("invalid_target:${parts[3]}"))
                } else {
                    Result.success(ProfileKey(index, key))
                }
            }
            else -> Result.failure(InvalidProfile("malformed_key:$key"))
        }
    }

    private fun String.toProfileId(): Result<AppearanceProfileId> =
        try {
            Result.success(AppearanceProfileId(this))
        } catch (_: IllegalArgumentException) {
            Result.failure(InvalidProfile("blank_profile_id"))
        }

    private fun String.toProfileIdOrNull(): Result<AppearanceProfileId?> =
        if (isEmpty()) {
            Result.success(null)
        } else {
            toProfileId()
        }

    private fun String.toProfileName(): Result<AppearanceProfileName> =
        try {
            Result.success(AppearanceProfileName(this))
        } catch (_: IllegalArgumentException) {
            Result.failure(InvalidProfile("blank_profile_name"))
        }

    private fun String.toFontFamily(): Result<AppearanceFontFamily> =
        try {
            Result.success(AppearanceFontFamily(this))
        } catch (_: IllegalArgumentException) {
            Result.failure(InvalidProfile("blank_font_family"))
        }

    private fun String.toColor(): Result<AppearanceColor> =
        try {
            Result.success(AppearanceColor.require(this))
        } catch (_: IllegalArgumentException) {
            Result.failure(InvalidProfile("invalid_colour"))
        }

    private fun String.toMode(): Result<AppearanceMode> =
        AppearanceMode.entries.firstOrNull { it.name == this }
            ?.let { Result.success(it) }
            ?: Result.failure(InvalidProfile("invalid_mode"))

    private fun String.escaped(): String =
        buildString {
            for (char in this@escaped) {
                when (char) {
                    '%' -> append("%25")
                    '\n' -> append("%0A")
                    '\r' -> append("%0D")
                    '=' -> append("%3D")
                    else -> append(char)
                }
            }
        }

    private fun String.decodeValue(): Result<String> {
        val output = StringBuilder()
        var index = 0
        while (index < length) {
            val char = this[index]
            if (char != '%') {
                output.append(char)
                index += 1
                continue
            }

            val escape = substring(index, (index + ESCAPE_LENGTH).coerceAtMost(length))
            val decoded = when (escape) {
                "%25" -> '%'
                "%0A" -> '\n'
                "%0D" -> '\r'
                "%3D" -> '='
                else -> return Result.failure(InvalidProfile("malformed_percent_escape"))
            }
            output.append(decoded)
            index += ESCAPE_LENGTH
        }
        return Result.success(output.toString())
    }

    private fun records(text: String): List<String> =
        text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .dropLastWhile { it.isEmpty() }

    private fun StringBuilder.appendKeyValue(
        key: String,
        value: String,
    ) {
        append(key)
        append(KEY_VALUE_SEPARATOR)
        append(value)
        append('\n')
    }

    private fun invalid(reason: String): AppearanceProfileStoreLoadResult.Invalid =
        AppearanceProfileStoreLoadResult.Invalid(reason)

    private fun invalid(error: Throwable): AppearanceProfileStoreLoadResult.Invalid =
        AppearanceProfileStoreLoadResult.Invalid(error.message ?: "invalid_profile")

    private data class ProfileKey(
        val index: Int,
        val field: String,
    )

    private class InvalidProfile(
        override val message: String,
    ) : Throwable(message)

    private companion object {
        const val FORMAT_VERSION_KEY: String = "formatVersion"
        const val FORMAT_VERSION: String = "1"
        const val ACTIVE_LIGHT_KEY: String = "activeProfile.light"
        const val ACTIVE_DARK_KEY: String = "activeProfile.dark"
        const val KEY_VALUE_SEPARATOR: Char = '='
        const val ESCAPE_LENGTH: Int = 3
        const val COLOR_FIELD: String = "color"
        val TOP_LEVEL_PROFILE_FIELDS: Set<String> = setOf("id", "name", "mode")
        val TEXT_PROFILE_FIELDS: Set<String> = setOf("font", "color")
        val profileComparator: Comparator<AppearanceProfile> =
            compareBy<AppearanceProfile> { it.mode.ordinal }
                .thenBy { it.name.value.lowercase() }
                .thenBy { it.id.value }
        val COMPATIBLE_MISSING_ELEMENT_TARGETS: Set<AppearanceElementTarget> = setOf(
            AppearanceElementTarget.ACCENT_TEXT,
            AppearanceElementTarget.ERROR_TEXT,
            AppearanceElementTarget.STATUS_TEXT,
            AppearanceElementTarget.MENU_DISABLED_TEXT,
            AppearanceElementTarget.INTERACTIVE_HOVER_TEXT,
        )
    }
}
