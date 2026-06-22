package org.gem.build.localization

internal object GemLocalizationPluralRules {
    val reportCounts: List<Int> = listOf(0, 1, 2, 3, 5, 21, 22, 25, 249, 1_000_000)

    private val localeRules: List<LocalePluralRule> = listOf(
        LocalePluralRule(exactLocaleTags = setOf("pt-br"), formula = Formula.PORTUGUESE_BRAZILIAN),
        LocalePluralRule(exactLocaleTags = setOf("pt-pt"), formula = Formula.ROMANCE_MILLION),
        LocalePluralRule(
            languages = setOf("da", "de", "el", "en", "et", "fi", "hu", "nb", "nl", "nn", "sv", "tr"),
            formula = Formula.ONE_OTHER,
        ),
        LocalePluralRule(languages = setOf("es", "it"), formula = Formula.ROMANCE_MILLION),
        LocalePluralRule(languages = setOf("pt"), formula = Formula.PORTUGUESE_BRAZILIAN),
        LocalePluralRule(languages = setOf("fr"), formula = Formula.FRENCH),
        LocalePluralRule(languages = setOf("cs"), formula = Formula.CZECH),
        LocalePluralRule(languages = setOf("lt"), formula = Formula.LITHUANIAN),
        LocalePluralRule(languages = setOf("lv"), formula = Formula.LATVIAN),
        LocalePluralRule(languages = setOf("pl"), formula = Formula.POLISH),
        LocalePluralRule(languages = setOf("ro"), formula = Formula.ROMANIAN),
        LocalePluralRule(languages = setOf("uk"), formula = Formula.UKRAINIAN),
    )

    fun category(localeTag: String, count: Int): String {
        require(count >= 0) {
            "Plural count must be non-negative: $count"
        }
        val normalizedTag = localeTag.lowercase()
        val language = normalizedTag.substringBefore('-')
        val rule = localeRules.firstOrNull { rule ->
            rule.matches(normalizedTag = normalizedTag, language = language)
        } ?: error("Unsupported plural locale: $localeTag")
        return rule.formula.evaluate(count)
    }

    fun resolverSource(): String {
        val runtimeBranches = localeRules.joinToString(separator = "\n") { rule ->
            "                        ${rule.runtimeBranch()}"
        }
        val runtimeFunctions = Formula.values().joinToString(separator = "\n\n") { formula ->
            formula.runtimeFunctionSource.prependIndent("                ")
        }
        return """
            package org.gem.ui.text

            object GemPluralCategoryResolver {
                fun category(localeTag: String, count: Int): GemPluralCategory {
                    require(count >= 0) {
                        "Plural count must be non-negative: ${'$'}count"
                    }
                    val normalizedTag = localeTag.lowercase()
                    val language = normalizedTag.substringBefore('-')
                    return when {
$runtimeBranches
                        else -> error("Unsupported plural locale: ${'$'}localeTag")
                    }
                }

$runtimeFunctions
            }
        """.trimIndent() + "\n"
    }

    private data class LocalePluralRule(
        val exactLocaleTags: Set<String> = emptySet(),
        val languages: Set<String> = emptySet(),
        val formula: Formula,
    ) {
        init {
            require(exactLocaleTags.isNotEmpty() || languages.isNotEmpty()) {
                "Plural rule must target an exact locale tag or language"
            }
        }

        fun matches(normalizedTag: String, language: String): Boolean =
            normalizedTag in exactLocaleTags || language in languages

        fun runtimeBranch(): String {
            val predicates = buildList {
                if (exactLocaleTags.isNotEmpty()) {
                    add("normalizedTag in ${exactLocaleTags.kotlinStringSet()}")
                }
                if (languages.isNotEmpty()) {
                    add("language in ${languages.kotlinStringSet()}")
                }
            }
            return "${predicates.joinToString(separator = " || ")} -> ${formula.runtimeFunctionName}(count)"
        }
    }

    private enum class Formula(
        val runtimeFunctionName: String,
        val runtimeFunctionSource: String,
        val evaluate: (Int) -> String,
    ) {
        ONE_OTHER(
            runtimeFunctionName = "oneOther",
            runtimeFunctionSource = """
                private fun oneOther(count: Int): GemPluralCategory =
                    if (count == 1) GemPluralCategory.ONE else GemPluralCategory.OTHER
            """.trimIndent(),
            evaluate = { count -> if (count == 1) "one" else "other" },
        ),
        ROMANCE_MILLION(
            runtimeFunctionName = "romanceMillion",
            runtimeFunctionSource = """
                private fun romanceMillion(count: Int): GemPluralCategory =
                    when {
                        count == 1 -> GemPluralCategory.ONE
                        count > 0 && count % 1_000_000 == 0 -> GemPluralCategory.MANY
                        else -> GemPluralCategory.OTHER
                    }
            """.trimIndent(),
            evaluate = { count ->
                when {
                    count == 1 -> "one"
                    count > 0 && count % 1_000_000 == 0 -> "many"
                    else -> "other"
                }
            },
        ),
        PORTUGUESE_BRAZILIAN(
            runtimeFunctionName = "portugueseBrazilian",
            runtimeFunctionSource = """
                private fun portugueseBrazilian(count: Int): GemPluralCategory =
                    when {
                        count == 0 || count == 1 -> GemPluralCategory.ONE
                        count > 0 && count % 1_000_000 == 0 -> GemPluralCategory.MANY
                        else -> GemPluralCategory.OTHER
                    }
            """.trimIndent(),
            evaluate = { count ->
                when {
                    count == 0 || count == 1 -> "one"
                    count > 0 && count % 1_000_000 == 0 -> "many"
                    else -> "other"
                }
            },
        ),
        FRENCH(
            runtimeFunctionName = "french",
            runtimeFunctionSource = """
                private fun french(count: Int): GemPluralCategory =
                    when {
                        count == 0 || count == 1 -> GemPluralCategory.ONE
                        count > 0 && count % 1_000_000 == 0 -> GemPluralCategory.MANY
                        else -> GemPluralCategory.OTHER
                    }
            """.trimIndent(),
            evaluate = { count ->
                when {
                    count == 0 || count == 1 -> "one"
                    count > 0 && count % 1_000_000 == 0 -> "many"
                    else -> "other"
                }
            },
        ),
        CZECH(
            runtimeFunctionName = "czech",
            runtimeFunctionSource = """
                private fun czech(count: Int): GemPluralCategory =
                    when (count) {
                        1 -> GemPluralCategory.ONE
                        in 2..4 -> GemPluralCategory.FEW
                        else -> GemPluralCategory.OTHER
                    }
            """.trimIndent(),
            evaluate = { count ->
                when (count) {
                    1 -> "one"
                    in 2..4 -> "few"
                    else -> "other"
                }
            },
        ),
        LITHUANIAN(
            runtimeFunctionName = "lithuanian",
            runtimeFunctionSource = """
                private fun lithuanian(count: Int): GemPluralCategory {
                    val mod10 = count % 10
                    val mod100 = count % 100
                    return when {
                        mod10 == 1 && mod100 !in 11..19 -> GemPluralCategory.ONE
                        mod10 in 2..9 && mod100 !in 11..19 -> GemPluralCategory.FEW
                        else -> GemPluralCategory.OTHER
                    }
                }
            """.trimIndent(),
            evaluate = { count ->
                val mod10 = count % 10
                val mod100 = count % 100
                when {
                    mod10 == 1 && mod100 !in 11..19 -> "one"
                    mod10 in 2..9 && mod100 !in 11..19 -> "few"
                    else -> "other"
                }
            },
        ),
        LATVIAN(
            runtimeFunctionName = "latvian",
            runtimeFunctionSource = """
                private fun latvian(count: Int): GemPluralCategory {
                    val mod10 = count % 10
                    val mod100 = count % 100
                    return when {
                        mod10 == 0 || mod100 in 11..19 -> GemPluralCategory.ZERO
                        mod10 == 1 && mod100 != 11 -> GemPluralCategory.ONE
                        else -> GemPluralCategory.OTHER
                    }
                }
            """.trimIndent(),
            evaluate = { count ->
                val mod10 = count % 10
                val mod100 = count % 100
                when {
                    mod10 == 0 || mod100 in 11..19 -> "zero"
                    mod10 == 1 && mod100 != 11 -> "one"
                    else -> "other"
                }
            },
        ),
        POLISH(
            runtimeFunctionName = "polish",
            runtimeFunctionSource = """
                private fun polish(count: Int): GemPluralCategory {
                    val mod10 = count % 10
                    val mod100 = count % 100
                    return when {
                        count == 1 -> GemPluralCategory.ONE
                        mod10 in 2..4 && mod100 !in 12..14 -> GemPluralCategory.FEW
                        count != 1 && (mod10 in 0..1 || mod10 in 5..9 || mod100 in 12..14) -> GemPluralCategory.MANY
                        else -> GemPluralCategory.OTHER
                    }
                }
            """.trimIndent(),
            evaluate = { count ->
                val mod10 = count % 10
                val mod100 = count % 100
                when {
                    count == 1 -> "one"
                    mod10 in 2..4 && mod100 !in 12..14 -> "few"
                    count != 1 && (mod10 in 0..1 || mod10 in 5..9 || mod100 in 12..14) -> "many"
                    else -> "other"
                }
            },
        ),
        ROMANIAN(
            runtimeFunctionName = "romanian",
            runtimeFunctionSource = """
                private fun romanian(count: Int): GemPluralCategory {
                    val mod100 = count % 100
                    return when {
                        count == 1 -> GemPluralCategory.ONE
                        count == 0 || mod100 in 1..19 -> GemPluralCategory.FEW
                        else -> GemPluralCategory.OTHER
                    }
                }
            """.trimIndent(),
            evaluate = { count ->
                val mod100 = count % 100
                when {
                    count == 1 -> "one"
                    count == 0 || mod100 in 1..19 -> "few"
                    else -> "other"
                }
            },
        ),
        UKRAINIAN(
            runtimeFunctionName = "ukrainian",
            runtimeFunctionSource = """
                private fun ukrainian(count: Int): GemPluralCategory {
                    val mod10 = count % 10
                    val mod100 = count % 100
                    return when {
                        mod10 == 1 && mod100 != 11 -> GemPluralCategory.ONE
                        mod10 in 2..4 && mod100 !in 12..14 -> GemPluralCategory.FEW
                        mod10 == 0 || mod10 in 5..9 || mod100 in 11..14 -> GemPluralCategory.MANY
                        else -> GemPluralCategory.OTHER
                    }
                }
            """.trimIndent(),
            evaluate = { count ->
                val mod10 = count % 10
                val mod100 = count % 100
                when {
                    mod10 == 1 && mod100 != 11 -> "one"
                    mod10 in 2..4 && mod100 !in 12..14 -> "few"
                    mod10 == 0 || mod10 in 5..9 || mod100 in 11..14 -> "many"
                    else -> "other"
                }
            },
        ),
    }

    private fun Set<String>.kotlinStringSet(): String =
        sorted().joinToString(prefix = "setOf(", postfix = ")") { value -> "\"$value\"" }
}
