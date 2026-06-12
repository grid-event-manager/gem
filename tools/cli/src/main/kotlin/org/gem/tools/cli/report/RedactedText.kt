package org.gem.tools.cli.report

import org.gem.core.services.SafeDiagnosticRedaction

@JvmInline
value class RedactedText private constructor(val value: String) {
    companion object {
        fun from(key: String, rawValue: String): RedactedText =
            RedactedText(SafeDiagnosticRedaction.redact(key, rawValue))

        fun map(values: Map<String, String>): Map<String, String> =
            SafeDiagnosticRedaction.redactMap(values)
    }
}
