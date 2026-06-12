package org.gem.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemDesktopSingleInstanceGuardTest {
    @Test
    fun `terminates other current Gem launcher instances but not current process`() {
        val current = FakeDesktopProcess(pid = 10, command = "/opt/gema/bin/gema")
        val older = FakeDesktopProcess(pid = 11, command = "/usr/bin/gema")
        val unrelated = FakeDesktopProcess(pid = 12, command = "/usr/bin/other-app")

        val report = GemDesktopSingleInstanceGuard.terminateOtherInstances(
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
    fun `terminates exact legacy Gem and Hostess launcher and main class instances`() {
        val legacyGemLauncher = FakeDesktopProcess(pid = 20, command = "/opt/gem/bin/gem")
        val legacyLauncher = FakeDesktopProcess(pid = 21, command = "/opt/hostess/bin/hostess")
        val legacyWindowsLauncher = FakeDesktopProcess(pid = 22, command = "C:\\Program Files\\Hostess\\hostess.exe")
        val legacyMainClass = FakeDesktopProcess(
            pid = 23,
            command = "/usr/bin/java",
            commandLine = "java ${GemDesktopSingleInstanceGuard.LegacyMainClassMarker}",
        )

        val report = GemDesktopSingleInstanceGuard.terminateOtherInstances(
            processes = listOf(legacyGemLauncher, legacyLauncher, legacyWindowsLauncher, legacyMainClass),
            currentPid = 24,
        )

        assertEquals(4, report.matchedProcessCount)
        assertEquals(4, report.terminatedProcessCount)
        assertTrue(legacyGemLauncher.destroyCalled)
        assertTrue(legacyLauncher.destroyCalled)
        assertTrue(legacyWindowsLauncher.destroyCalled)
        assertTrue(legacyMainClass.destroyCalled)
    }

    @Test
    fun `does not terminate similarly named unrelated commands`() {
        val gemini = FakeDesktopProcess(pid = 30, command = "/usr/bin/gemini")
        val hostessHelper = FakeDesktopProcess(pid = 31, command = "/usr/bin/hostess-helper")
        val arbitrary = FakeDesktopProcess(pid = 32, command = "/usr/bin/random")

        val report = GemDesktopSingleInstanceGuard.terminateOtherInstances(
            processes = listOf(gemini, hostessHelper, arbitrary),
            currentPid = 33,
        )

        assertEquals(0, report.matchedProcessCount)
        assertEquals(0, report.terminatedProcessCount)
        assertFalse(gemini.destroyCalled)
        assertFalse(hostessHelper.destroyCalled)
        assertFalse(arbitrary.destroyCalled)
    }

    @Test
    fun `terminates development main class instance when graceful shutdown times out`() {
        val process = FakeDesktopProcess(
            pid = 40,
            command = "/usr/bin/java",
            commandLine = "java ${GemDesktopSingleInstanceGuard.CurrentMainClassMarker}",
            gracefulExit = false,
        )

        val report = GemDesktopSingleInstanceGuard.terminateOtherInstances(
            processes = listOf(process),
            currentPid = 41,
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
    ) : GemDesktopProcess {
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
