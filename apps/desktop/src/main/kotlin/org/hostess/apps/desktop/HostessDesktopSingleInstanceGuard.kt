package org.hostess.apps.desktop

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object HostessDesktopSingleInstanceGuard {
    fun terminateOtherInstances(): HostessDesktopSingleInstanceReport =
        terminateOtherInstances(
            processes = ProcessHandle.allProcesses()
                .map(::ProcessHandleDesktopProcess)
                .toList(),
            currentPid = ProcessHandle.current().pid(),
        )

    internal fun terminateOtherInstances(
        processes: List<HostessDesktopProcess>,
        currentPid: Long,
    ): HostessDesktopSingleInstanceReport {
        val targets = processes
            .filter { process -> process.pid != currentPid && process.isHostessDesktopProcess() }

        val terminated = targets.count { process ->
            process.terminate()
        }

        return HostessDesktopSingleInstanceReport(
            scannedProcessCount = processes.size,
            matchedProcessCount = targets.size,
            terminatedProcessCount = terminated,
        )
    }

    private fun HostessDesktopProcess.isHostessDesktopProcess(): Boolean {
        val commandText = listOf(command, commandLine)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
        val commandName = command.substringAfterLast('/').substringAfterLast('\\')
        return commandText.contains(MainClassMarker) ||
            commandName.equals(LauncherName, ignoreCase = true) ||
            commandName.equals(WindowsLauncherName, ignoreCase = true)
    }

    private fun HostessDesktopProcess.terminate(): Boolean {
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

    private const val MainClassMarker: String = "org.hostess.apps.desktop.HostessDesktopAppKt"
    private const val LauncherName: String = "hostess"
    private const val WindowsLauncherName: String = "hostess.exe"
    private const val GracefulShutdownTimeoutMillis: Long = 2_000L
    private const val ForcedShutdownTimeoutMillis: Long = 1_000L
}

data class HostessDesktopSingleInstanceReport(
    val scannedProcessCount: Int,
    val matchedProcessCount: Int,
    val terminatedProcessCount: Int,
)

internal interface HostessDesktopProcess {
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
) : HostessDesktopProcess {
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
