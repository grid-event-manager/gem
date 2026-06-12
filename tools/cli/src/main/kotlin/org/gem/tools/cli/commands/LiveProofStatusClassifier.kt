package org.gem.tools.cli.commands

internal object LiveProofStatusClassifier {
    fun classify(detail: String): String {
        val lower = detail.lowercase()
        return when {
            "transport" in lower -> "transport_gap"
            "runtime" in lower || "unavailable" in lower -> "runtime_gap"
            "proof" in lower -> "proof_gap"
            "blocked" in lower -> "blocked"
            else -> "failed"
        }
    }
}
