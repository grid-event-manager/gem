package org.hostess.tools.cli.composition

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.OperatorLabel

class NoticeComplianceLedgersTest {
    @Test
    fun `file ledger snapshots read only then reserve and record rewrite same file`() {
        val directory = Files.createTempDirectory("hostess-notice-ledger")
        try {
            val path = directory.resolve("notice-ledger.tsv")
            val ledger = FileNoticeComplianceLedgerPort(path)
            val operator = OperatorLabel("test-operator")
            val day = NoticeDeliveryDay("2026-06-05")

            val snapshot = assertIs<NoticeComplianceLedgerResult.Success<NoticeDeliveryLedgerSnapshot>>(
                ledger.snapshot(operator, day),
            ).value
            assertEquals(NoticeDeliveryCount.ZERO, snapshot.reservedDeliveryCount)
            assertFalse(path.exists())

            ledger.reserve(operator, day, NoticeDeliveryCount(12))
            assertEquals("test-operator\t2026-06-05\t12\t0\n", path.readText())

            ledger.recordSendResult(operator, day, reservedProjection = NoticeDeliveryCount(12), delivered = NoticeDeliveryCount(5))
            assertEquals("test-operator\t2026-06-05\t0\t5\n", path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `unavailable ledger fails closed for every operation`() {
        val ledger = UnavailableNoticeComplianceLedgerPort()
        val operator = OperatorLabel("test-operator")
        val day = NoticeDeliveryDay("2026-06-05")

        assertIs<NoticeComplianceLedgerResult.Failure>(ledger.snapshot(operator, day))
        assertIs<NoticeComplianceLedgerResult.Failure>(ledger.reserve(operator, day, NoticeDeliveryCount(1)))
        assertIs<NoticeComplianceLedgerResult.Failure>(
            ledger.recordSendResult(operator, day, reservedProjection = NoticeDeliveryCount(1), delivered = NoticeDeliveryCount(1)),
        )
    }
}
