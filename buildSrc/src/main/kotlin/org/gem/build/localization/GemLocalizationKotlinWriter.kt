package org.gem.build.localization

import java.io.File

internal object GemLocalizationKotlinWriter {
    private const val TEXT_OVERRIDE_PREFIX = "override fun text"

    fun write(sources: List<GemLocalizationSource>, generatedSourceRoot: File, reportDirectory: File) {
        generatedSourceRoot.deleteRecursively()
        reportDirectory.deleteRecursively()
        val packageDirectory = File(generatedSourceRoot, "org/gem/ui/text")
        packageDirectory.mkdirs()
        reportDirectory.mkdirs()

        File(packageDirectory, "GeneratedGemTextCatalogues.kt").writeText(generatedSource(sources))
        sources.forEach { source ->
            writeReports(source, reportDirectory)
        }
    }

    private fun generatedSource(sources: List<GemLocalizationSource>): String {
        val registryRows = sources.joinToString(separator = ",\n") { source ->
            """
                GemTextCatalogueMetadata(
                    localeTag = ${source.localeTag.kotlinString()},
                    languageName = ${source.languageName.kotlinString()},
                    nativeName = ${source.nativeName.kotlinString()},
                    catalogue = ${catalogueObjectName(source)},
                )
            """.trimIndent().prependIndent("            ")
        }
        val catalogueObjects = sources.joinToString(separator = "\n\n") { source ->
            catalogueObject(source)
        }
        return """
            package org.gem.ui.text

            data class GemTextCatalogueMetadata(
                val localeTag: String,
                val languageName: String,
                val nativeName: String,
                val catalogue: GemTextCatalogue,
            )

            class GemTextCatalogueRegistry(
                localeMetadata: List<GemTextCatalogueMetadata>,
            ) {
                val locales: List<GemTextCatalogueMetadata> =
                    localeMetadata.sortedBy { it.localeTag }

                fun catalogueFor(localeTag: String): GemTextCatalogue? =
                    locales.firstOrNull { it.localeTag == localeTag }?.catalogue

                companion object {
                    private val generated = GemTextCatalogueRegistry(
                        listOf(
$registryRows
                        ),
                    )

                    val locales: List<GemTextCatalogueMetadata>
                        get() = generated.locales

                    fun catalogueFor(localeTag: String): GemTextCatalogue? =
                        generated.catalogueFor(localeTag)

                    fun generated(): GemTextCatalogueRegistry =
                        generated
                }
            }

            $catalogueObjects
        """.trimIndent() + "\n"
    }

    private fun catalogueObject(source: GemLocalizationSource): String {
        val fixedBranches = GemLocalizationContract.fixedKeys.joinToString(separator = "\n") { key ->
            "        GemTextKey.$key -> ${source.fixedValues.getValue(key).kotlinString()}"
        }
        return """
            object ${catalogueObjectName(source)} : GemTextCatalogue {
                $TEXT_OVERRIDE_PREFIX(key: GemTextKey): String = when (key) {
$fixedBranches
                    is GemTextKey.DraftCharCount -> formatDraftCharCount(key.count)
                    is GemTextKey.SelectedCount -> formatSelectedCount(key.count)
                    is GemTextKey.SendFailureDetailLine -> formatSendFailureDetailLine(key.groupName, key.reason)
                }

                private fun formatDraftCharCount(count: Int): String =
                    formatCount(${source.localeTag.kotlinString()}, count, ${source.countValues.getValue("DraftCharCount").kotlinMapLiteral()})

                private fun formatSelectedCount(count: Int): String =
                    formatCount(${source.localeTag.kotlinString()}, count, ${source.countValues.getValue("SelectedCount").kotlinMapLiteral()})

                private fun formatSendFailureDetailLine(groupName: String, reason: String): String =
                    ${source.placeholderValues.getValue("SendFailureDetailLine").kotlinString()}
                        .replace("{groupName}", groupName)
                        .replace("{reason}", reason)

                private fun formatCount(localeTag: String, count: Int, patterns: Map<GemPluralCategory, String>): String {
                    val category = GemPluralCategoryResolver.category(localeTag, count)
                    val template = patterns[category] ?: patterns.getValue(GemPluralCategory.OTHER)
                    return template.replace("{count}", count.toString())
                }
            }
        """.trimIndent()
    }

    private fun writeReports(source: GemLocalizationSource, reportDirectory: File) {
        File(reportDirectory, "${source.localeTag}-fixed-keys.tsv").writeText(
            buildString {
                appendLine("key\tvalue")
                GemLocalizationContract.fixedKeys.forEach { key ->
                    appendLine("$key\t${source.fixedValues.getValue(key)}")
                }
            },
        )
        File(reportDirectory, "${source.localeTag}-dynamic-keys.tsv").writeText(
            buildString {
                appendLine("key\tcase\tvalue")
                appendLine("DraftCharCount\t0\t${renderCount(source, "DraftCharCount", 0)}")
                appendLine("DraftCharCount\t1\t${renderCount(source, "DraftCharCount", 1)}")
                appendLine("DraftCharCount\t249\t${renderCount(source, "DraftCharCount", 249)}")
                appendLine("SelectedCount\t0\t${renderCount(source, "SelectedCount", 0)}")
                appendLine("SelectedCount\t1\t${renderCount(source, "SelectedCount", 1)}")
                appendLine("SelectedCount\t3\t${renderCount(source, "SelectedCount", 3)}")
                appendLine("SendFailureDetailLine\tOwks\t${source.placeholderValues.getValue("SendFailureDetailLine").replace("{groupName}", "Owks").replace("{reason}", "Second Life did not accept the notice send.")}")
            },
        )
    }

    private fun renderCount(source: GemLocalizationSource, key: String, count: Int): String {
        val category = if (source.localeTag.startsWith("en") && count == 1) "one" else "other"
        return source.countValues.getValue(key).getValue(category).replace("{count}", count.toString())
    }

    private fun catalogueObjectName(source: GemLocalizationSource): String =
        when (source.localeTag) {
            "en-GB" -> "EnglishGemTextCatalogue"
            else -> error("No generated catalogue object name for ${source.localeTag}")
        }

    private fun String.kotlinString(): String =
        buildString {
            append('"')
            this@kotlinString.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }

    private fun Map<String, String>.kotlinMapLiteral(): String =
        entries.joinToString(
            prefix = "mapOf(",
            postfix = ")",
            separator = ", ",
        ) { (category, value) ->
            "GemPluralCategory.${category.uppercase()} to ${value.kotlinString()}"
        }
}
