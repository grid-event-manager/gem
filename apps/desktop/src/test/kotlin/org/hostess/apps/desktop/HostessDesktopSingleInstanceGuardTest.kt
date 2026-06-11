package org.hostess.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostessDesktopSingleInstanceGuardTest {
    @Test
    fun `terminates other installed launcher instances but not current process`() {
        val current = FakeDesktopProcess(pid = 10, command = "/opt/hostess/bin/hostess")
        val older = FakeDesktopProcess(pid = 11, command = "/opt/hostess/bin/hostess")
        val unrelated = FakeDesktopProcess(pid = 12, command = "/usr/bin/other-app")

        val report = HostessDesktopSingleInstanceGuard.terminateOtherInstances(
            processes = listOf(current, older, unrelated),
            currentPid = current.pid,
        )

        assertEquals(3, report.scannedProcessCount)
        assertEquals(1, report.matchedProcessCount)
        assertEquals(1, report.terminatedProcessCount)
        assertFalse(current.destroyCalled)
        assertTrue(older.destroyCalled)
        assertFalse(unrelated.destroyCalled)
    }

    @Test
    fun `terminates development main class instance when graceful shutdown times out`() {
        val process = FakeDesktopProcess(
            pid = 20,
            command = "/usr/bin/java",
            commandLine = "java org.hostess.apps.desktop.HostessDesktopAppKt",
            gracefulExit = false,
        )

        val report = HostessDesktopSingleInstanceGuard.terminateOtherInstances(
            processes = listOf(process),
            currentPid = 21,
        )

        assertEquals(1, report.matchedProcessCount)
        assertEquals(1, report.terminatedProcessCount)
        assertTrue(process.destroyCalled)
        assertTrue(process.destroyForciblyCalled)
    }

    private class FakeDesktopProcess(
        override val pid: Long,
        override val command: String,
        override val commandLine: String = command,
        private val gracefulExit: Boolean = true,
    ) : HostessDesktopProcess {
        var destroyCalled: Boolean = false
        var destroyForciblyCalled: Boolean = false
        private var alive: Boolean = true

        override fun isAlive(): Boolean = alive

        override fun destroy(): Boolean {
            destroyCalled = true
            if (gracefulExit) {
                alive = false
            }
            return true
        }

        override fun destroyForcibly(): Boolean {
            destroyForciblyCalled = true
            alive = false
            return true
        }

        override fun awaitExit(timeoutMillis: Long): Boolean =
            !alive
    }
}
