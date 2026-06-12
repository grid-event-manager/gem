package org.gem.apps.desktop

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object GemDesktopSingleInstanceGuard {
    fun terminateOtherInstances(): GemDesktopSingleInstanceReport =
        terminateOtherInstances(
            processes = ProcessHandle.allProcesses()
                .map(::ProcessHandleDesktopProcess)
                .toList(),
            currentPid = ProcessHandle.current().pid(),
        )

    internal fun terminateOtherInstances(
        processes: List<GemDesktopProcess>,
        currentPid: Long,
    ): GemDesktopSingleInstanceReport {
        val targets = processes
            .filter { process -> process.pid != currentPid && process.isGemDesktopProcess() }

        val terminated = targets.count { process ->
            process.terminate()
        }

        return GemDesktopSingleInstanceReport(
            scannedProcessCount = processes.size,
            matchedProcessCount = targets.size,
            terminatedProcessCount = terminated,
        )
    }

    private fun GemDesktopProcess.isGemDesktopProcess(): Boolean {
        val commandText = listOf(command, commandLine)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
        val commandName = command.substringAfterLast('/').substringAfterLast('\\')
        return commandText.contains(CurrentMainClassMarker) ||
            commandText.contains(LegacyMainClassMarker) ||
            CurrentLauncherNames.any { launcher -> commandName.equals(launcher, ignoreCase = true) } ||
            LegacyLauncherNames.any { launcher -> commandName.equals(launcher, ignoreCase = true) }
    }

    private fun GemDesktopProcess.terminate(): Boolean {
        if (!isAlive()) {
            return true
        }
        destroy()
        if (awaitExit(GracefulShutdownTimeoutMillis)) {
            return true
        }
        destroyForcibly()
        return awaitExit(ForcedShutdownTimeoutMillis) || !isAlive()
    }

    internal const val CurrentMainClassMarker: String = "org.gem.apps.desktop.GemDesktopAppKt"
    internal val LegacyMainClassMarker: String =
        "org.$LegacyPackageSegment.apps.desktop.${LegacyTypePrefix}DesktopAppKt"
    private val CurrentLauncherNames: Set<String> = setOf("gem", "gem.exe")
    private val LegacyLauncherNames: Set<String> = setOf(LegacyPackageSegment, "$LegacyPackageSegment.exe")
    private const val LegacyPackageSegment: String = "hostess"
    private const val LegacyTypePrefix: String = "Hostess"
    private const val GracefulShutdownTimeoutMillis: Long = 2_000L
    private const val ForcedShutdownTimeoutMillis: Long = 1_000L
}

data class GemDesktopSingleInstanceReport(
    val scannedProcessCount: Int,
    val matchedProcessCount: Int,
    val terminatedProcessCount: Int,
)

internal interface GemDesktopProcess {
    val pid: Long
    val command: String
    val commandLine: String
    fun isAlive(): Boolean
    fun destroy(): Boolean
    fun destroyForcibly(): Boolean
    fun awaitExit(timeoutMillis: Long): Boolean
}

private class ProcessHandleDesktopProcess(
    private val process: ProcessHandle,
) : GemDesktopProcess {
    override val pid: Long = process.pid()
    override val command: String = process.info().command().orElse("")
    override val commandLine: String = process.info().commandLine().orElse("")

    override fun isAlive(): Boolean =
        process.isAlive

    override fun destroy(): Boolean =
        process.destroy()

    override fun destroyForcibly(): Boolean =
        process.destroyForcibly()

    override fun awaitExit(timeoutMillis: Long): Boolean =
        if (!process.isAlive) {
            true
        } else {
            try {
                process.onExit().get(timeoutMillis, TimeUnit.MILLISECONDS)
                true
            } catch (_: TimeoutException) {
                false
            }
        }
}
