package org.gem.build.localization

import java.io.File
import java.util.Properties

internal object GemLocalizationPropertiesParser {
    private val placeholderPattern = Regex("\\{([A-Za-z][A-Za-z0-9]*)}")

    fun parseDirectory(sourceDirectory: File): List<GemLocalizationSource> {
        require(sourceDirectory.isDirectory) {
            "Localization source directory missing: ${sourceDirectory.absolutePath}"
        }
        val files = sourceDirectory.listFiles { file ->
            file.isFile && file.extension == "properties"
        }?.sortedBy { it.name }.orEmpty()
        require(files.isNotEmpty()) {
            "No localization source files found in ${sourceDirectory.absolutePath}"
        }
        val sources = files.map(::parseFile)
        val duplicateLocale = sources
            .groupBy { it.localeTag }
            .filterValues { it.size > 1 }
            .keys
            .firstOrNull()
        require(duplicateLocale == null) {
            "Duplicate localization locale tag: $duplicateLocale"
        }
        return sources
    }

    private fun parseFile(file: File): GemLocalizationSource {
        val rawLines = file.readLines()
        val rawKeys = rawLines.mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!") -> null
                else -> {
                    val separator = line.indexOf('=')
                    require(separator >= 0) {
                        "Invalid localization line ${index + 1} in ${file.name}: missing '='"
                    }
                    line.substring(0, separator).trim()
                }
            }
        }
        val duplicateKey = rawKeys
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .firstOrNull()
        require(duplicateKey == null) {
            "Duplicate localization key in ${file.name}: $duplicateKey"
        }

        val properties = Properties().apply {
            file.reader(Charsets.UTF_8).use(::load)
        }
        val values = rawKeys.associateWith { key ->
            properties.getProperty(key) ?: error("Localization key missing after load in ${file.name}: $key")
        }

        val localeTag = requiredValue(file, values, "meta.locale")
        require(file.nameWithoutExtension == localeTag) {
            "Localization file ${file.name} meta.locale must match filename: $localeTag"
        }
        require(localeTag == "en-GB") {
            "Track D only authorizes en-GB production localization source: $localeTag"
        }
        val requiredPluralCategories = GemLocalizationContract.requiredPluralCategoriesByLocale[localeTag]
            ?: error("Missing plural formula contract for locale: $localeTag")

        val allowedKeys = buildSet {
            add("meta.locale")
            add("meta.languageName")
            add("meta.nativeName")
            addAll(GemLocalizationContract.fixedKeys)
            addAll(GemLocalizationContract.placeholderKeys.keys)
            GemLocalizationContract.countKeys.forEach { key ->
                GemLocalizationContract.pluralCategories.forEach { category ->
                    add("$key.$category")
                }
            }
        }

        val unknownKey = values.keys.firstOrNull { it !in allowedKeys && !it.startsWith("meta.") }
        require(unknownKey == null) {
            "Unknown localization key in ${file.name}: $unknownKey"
        }

        GemLocalizationContract.fixedKeys.forEach { key ->
            require(values.containsKey(key)) {
                "Missing fixed localization key in ${file.name}: $key"
            }
        }
        GemLocalizationContract.protectedProductValues.forEach { (key, expected) ->
            require(values[key] == expected) {
                "Protected product localization key changed in ${file.name}: $key"
            }
        }
        GemLocalizationContract.placeholderKeys.forEach { (key, placeholders) ->
            val value = requiredValue(file, values, key)
            require(placeholdersIn(value) == placeholders) {
                "Placeholder mismatch for $key in ${file.name}: expected $placeholders"
            }
        }

        val countValues = GemLocalizationContract.countKeys.associateWith { key ->
            val patterns = requiredPluralCategories.associateWith { category ->
                requiredValue(file, values, "$key.$category")
            }
            require(patterns.containsKey("other")) {
                "Missing required other plural for $key in ${file.name}"
            }
            patterns.values.forEach { value ->
                require(placeholdersIn(value) == setOf("count")) {
                    "Placeholder mismatch for $key in ${file.name}: expected count"
                }
            }
            patterns
        }

        return GemLocalizationSource(
            localeTag = localeTag,
            languageName = requiredValue(file, values, "meta.languageName"),
            nativeName = requiredValue(file, values, "meta.nativeName"),
            fixedValues = GemLocalizationContract.fixedKeys.associateWith { key -> values.getValue(key) },
            countValues = countValues,
            placeholderValues = GemLocalizationContract.placeholderKeys.keys.associateWith { key -> values.getValue(key) },
        )
    }

    private fun requiredValue(file: File, values: Map<String, String>, key: String): String =
        values[key] ?: error("Missing localization key in ${file.name}: $key")

    private fun placeholdersIn(value: String): Set<String> =
        placeholderPattern.findAll(value).map { it.groupValues[1] }.toSet()
}
