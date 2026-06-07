package org.hostess.tools.cli.composition

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionCount
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.domain.OperatorLabel

class NoticeComplianceLedgersTest {
    @Test
    fun `file ledger snapshots read only then reserve and record per group`() {
        val directory = Files.createTempDirectory("hostess-notice-ledger")
        try {
            val path = directory.resolve("notice-ledger.tsv")
            val ledger = FileNoticeSubmissionLedgerPort(path)
            val account = AccountLabel("proof-account")
            val operator = OperatorLabel("test-operator")
            val day = NoticeLedgerDay("2026-06-05")
            val groups = listOf(group("venue-hosts", "Venue Hosts"), group("event-notices", "Event Notices"))
            val projection = NoticeSubmissionProjection.from(groups)

            val snapshots = assertIs<NoticeComplianceLedgerResult.Success<*>>(
                ledger.snapshot(account, operator, groups, day),
            ).value as List<*>
            assertEquals(2, snapshots.size)
            assertFalse(path.exists())

            ledger.reserve(account, operator, projection, day)
            assertEquals(
                "proof-account\tevent-notices\tEvent Notices\t2026-06-05\t1\t0\ttest-operator\n" +
                    "proof-account\tvenue-hosts\tVenue Hosts\t2026-06-05\t1\t0\ttest-operator\n",
                path.readText(),
            )

            ledger.recordSendResult(account, operator, projection, sentGroups = listOf(groups.first()), day)
            assertEquals(
                "proof-account\tevent-notices\tEvent Notices\t2026-06-05\t0\t0\ttest-operator\n" +
                    "proof-account\tvenue-hosts\tVenue Hosts\t2026-06-05\t0\t1\ttest-operator\n",
                path.readText(),
            )

            val afterRecord = when (val result = ledger.snapshot(account, operator, groups, day)) {
                is NoticeComplianceLedgerResult.Success -> result.value
                is NoticeComplianceLedgerResult.Failure -> error("snapshot failed: ${result.reasonCode}")
            }
            assertEquals(listOf(NoticeSubmissionCount(1), NoticeSubmissionCount.ZERO), afterRecord.map { it.recordedSentSubmissionCount })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `unavailable ledger fails closed for every operation`() {
        val ledger = UnavailableNoticeSubmissionLedgerPort()
        val account = AccountLabel("proof-account")
        val operator = OperatorLabel("test-operator")
        val day = NoticeLedgerDay("2026-06-05")
        val groups = listOf(group("venue-hosts", "Venue Hosts"))
        val projection = NoticeSubmissionProjection.from(groups)

        assertIs<NoticeComplianceLedgerResult.Failure>(ledger.snapshot(account, operator, groups, day))
        assertIs<NoticeComplianceLedgerResult.Failure>(ledger.reserve(account, operator, projection, day))
        assertIs<NoticeComplianceLedgerResult.Failure>(
            ledger.recordSendResult(account, operator, projection, sentGroups = groups, day),
        )
    }

    private fun group(id: String, displayName: String): GroupMembership =
        GroupMembership.fromValues(id, displayName, canSendNotices = true, acceptsNotices = true)
}
