package org.gem.protocol.libomv.transport

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object SimulatorUdpDiagnosticTrail {
    private const val ENABLED_PROPERTY = "gem.simulator.udp.diagnostics"
    private const val PATH_PROPERTY = "gem.simulator.udp.diagnostics.path"
    private const val TRUE = "true"
    private const val LOG_FILE_PREFIX = "simulator-udp"
    private const val LOG_FILE_EXTENSION = "log"
    private const val MAX_VALUE_LENGTH = 320
    private const val MAC_OS_NAME = "mac"
    private val lock = Any()
    private var writer: BufferedWriter? = null

    fun enabled(): Boolean =
        System.getProperty(ENABLED_PROPERTY)?.equals(TRUE, ignoreCase = true) == true

    fun record(event: String, vararg fields: Pair<String, Any?>) {
        if (!enabled()) {
            return
        }
        synchronized(lock) {
            val activeWriter = writer ?: openWriter() ?: return
            runCatching {
                activeWriter.write(formatLine(event, fields.toList()))
                activeWriter.newLine()
                activeWriter.flush()
            }
        }
    }

    fun closeForTests() {
        synchronized(lock) {
            runCatching { writer?.close() }
            writer = null
        }
    }

    private fun openWriter(): BufferedWriter? =
        runCatching {
            val directory = diagnosticDirectory()
            directory.mkdirs()
            val file = directory.resolve("${LOG_FILE_PREFIX}-${fileTimestamp()}-${System.nanoTime()}.$LOG_FILE_EXTENSION")
            BufferedWriter(FileWriter(file, true)).also { activeWriter ->
                writer = activeWriter
                activeWriter.write(
                    formatLine(
                        event = "diagnostic_log_opened",
                        fields = listOf(
                            "path" to file.absolutePath,
                            "osName" to System.getProperty("os.name"),
                            "osVersion" to System.getProperty("os.version"),
                            "osArch" to System.getProperty("os.arch"),
                            "javaVersion" to System.getProperty("java.version"),
                            "javaVendor" to System.getProperty("java.vendor"),
                        ),
                    ),
                )
                activeWriter.newLine()
                activeWriter.flush()
            }
        }.getOrNull()

    private fun diagnosticDirectory(): File {
        System.getProperty(PATH_PROPERTY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { return File(it) }

        val home = System.getProperty("user.home")?.takeIf(String::isNotBlank) ?: "."
        val osName = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        return if (MAC_OS_NAME in osName) {
            File(home, "Library/Application Support/gem/diagnostics")
        } else {
            File(home, ".local/share/gem/diagnostics")
        }
    }

    private fun formatLine(event: String, fields: List<Pair<String, Any?>>): String =
        buildList {
            add("timestamp=${lineTimestamp()}")
            add("thread=${sanitize(Thread.currentThread().name)}")
            add("event=${sanitize(event)}")
            fields.forEach { (key, value) ->
                add("${sanitize(key)}=${sanitize(value?.toString() ?: "<null>")}")
            }
        }.joinToString(" ")

    private fun sanitize(value: String): String =
        value
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .let { if (it.length > MAX_VALUE_LENGTH) it.take(MAX_VALUE_LENGTH) else it }

    private fun lineTimestamp(): String =
        timestampFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Date())

    private fun fileTimestamp(): String =
        timestampFormatter("yyyyMMdd-HHmmss-SSS").format(Date())

    private fun timestampFormatter(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}
