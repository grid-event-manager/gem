package org.hostess.tools.cli.composition

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionCount
import org.hostess.core.domain.NoticeSubmissionLedgerSnapshot
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.ports.NoticeSubmissionLedgerPort

internal class InMemoryNoticeSubmissionLedgerPort : NoticeSubmissionLedgerPort {
    private val snapshots = linkedMapOf<LedgerKey, NoticeSubmissionLedgerSnapshot>()

    override fun snapshot(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        groups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        NoticeComplianceLedgerResult.Success(
            groups.map { group -> currentSnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay) },
        )

    override fun reserve(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        NoticeComplianceLedgerResult.Success(
            projection.selectedGroups.map { group ->
                val current = currentSnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay)
                val updated = current.copy(
                    groupDisplayName = group.displayName,
                    reservedSubmissionCount = current.reservedSubmissionCount.plus(NoticeSubmissionCount.ONE),
                    lastOperatorLabel = operatorLabel,
                )
                snapshots[key(proofAccountLabel, group, noticeLedgerDay)] = updated
                updated
            },
        )

    override fun recordSendResult(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        sentGroups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
        if (!projection.containsAll(sentGroups)) {
            return NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }
        val sentIds = sentGroups.mapTo(hashSetOf()) { it.groupId }
        return NoticeComplianceLedgerResult.Success(
            projection.selectedGroups.map { group ->
                val current = currentSnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay)
                val updated = current.copy(
                    groupDisplayName = group.displayName,
                    reservedSubmissionCount = current.reservedSubmissionCount.minusFloorZero(NoticeSubmissionCount.ONE),
                    recordedSentSubmissionCount = if (group.groupId in sentIds) {
                        current.recordedSentSubmissionCount.plus(NoticeSubmissionCount.ONE)
                    } else {
                        current.recordedSentSubmissionCount
                    },
                    lastOperatorLabel = operatorLabel,
                )
                snapshots[key(proofAccountLabel, group, noticeLedgerDay)] = updated
                updated
            },
        )
    }

    private fun currentSnapshot(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        group: GroupMembership,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeSubmissionLedgerSnapshot =
        snapshots[key(proofAccountLabel, group, noticeLedgerDay)]
            ?: emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay)
}

internal class UnavailableNoticeSubmissionLedgerPort : NoticeSubmissionLedgerPort {
    override fun snapshot(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        groups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")

    override fun reserve(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")

    override fun recordSendResult(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        sentGroups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
}

internal class FileNoticeSubmissionLedgerPort(
    private val path: Path,
) : NoticeSubmissionLedgerPort {
    override fun snapshot(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        groups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        try {
            val snapshots = readSnapshots()
            NoticeComplianceLedgerResult.Success(
                groups.map { group ->
                    snapshots[key(proofAccountLabel, group, noticeLedgerDay)]
                        ?: emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay)
                },
            )
        } catch (_: Exception) {
            NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }

    override fun reserve(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        mutate(proofAccountLabel, operatorLabel, projection, noticeLedgerDay) { current, group, _ ->
            current.copy(
                groupDisplayName = group.displayName,
                reservedSubmissionCount = current.reservedSubmissionCount.plus(NoticeSubmissionCount.ONE),
                lastOperatorLabel = operatorLabel,
            )
        }

    override fun recordSendResult(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        sentGroups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
        if (!projection.containsAll(sentGroups)) {
            return NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }
        val sentIds = sentGroups.mapTo(hashSetOf()) { it.groupId }
        return mutate(proofAccountLabel, operatorLabel, projection, noticeLedgerDay) { current, group, _ ->
            current.copy(
                groupDisplayName = group.displayName,
                reservedSubmissionCount = current.reservedSubmissionCount.minusFloorZero(NoticeSubmissionCount.ONE),
                recordedSentSubmissionCount = if (group.groupId in sentIds) {
                    current.recordedSentSubmissionCount.plus(NoticeSubmissionCount.ONE)
                } else {
                    current.recordedSentSubmissionCount
                },
                lastOperatorLabel = operatorLabel,
            )
        }
    }

    private fun mutate(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        noticeLedgerDay: NoticeLedgerDay,
        action: (NoticeSubmissionLedgerSnapshot, GroupMembership, LedgerKey) -> NoticeSubmissionLedgerSnapshot,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
        try {
            val snapshots = readSnapshots().toMutableMap()
            val updated = projection.selectedGroups.map { group ->
                val key = key(proofAccountLabel, group, noticeLedgerDay)
                val current = snapshots[key] ?: emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay)
                action(current, group, key).also { snapshots[key] = it }
            }
            writeSnapshots(snapshots)
            NoticeComplianceLedgerResult.Success(updated)
        } catch (_: Exception) {
            NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }

    private fun readSnapshots(): Map<LedgerKey, NoticeSubmissionLedgerSnapshot> {
        if (!path.exists()) {
            return emptyMap()
        }

        return Files.readAllLines(path, StandardCharsets.UTF_8)
            .filter(String::isNotBlank)
            .associate { row ->
                val parts = row.split('\t')
                require(parts.size == 7) { "malformed notice ledger row" }
                val snapshot = NoticeSubmissionLedgerSnapshot(
                    proofAccountLabel = AccountLabel(parts[0]),
                    groupId = GroupId(parts[1]),
                    groupDisplayName = GroupDisplayName(parts[2]),
                    noticeLedgerDay = NoticeLedgerDay(parts[3]),
                    reservedSubmissionCount = NoticeSubmissionCount(parts[4].toLong()),
                    recordedSentSubmissionCount = NoticeSubmissionCount(parts[5].toLong()),
                    lastOperatorLabel = OperatorLabel(parts[6]),
                )
                key(snapshot.proofAccountLabel, snapshot.groupId, snapshot.noticeLedgerDay) to snapshot
            }
    }

    private fun writeSnapshots(snapshots: Map<LedgerKey, NoticeSubmissionLedgerSnapshot>) {
        path.parent?.let(Files::createDirectories)
        val body = snapshots.entries
            .sortedWith(compareBy({ it.key.proofAccount }, { it.key.groupId }, { it.key.day }))
            .joinToString(separator = "\n", postfix = "\n") { (_, snapshot) ->
                listOf(
                    snapshot.proofAccountLabel.value,
                    snapshot.groupId.value,
                    snapshot.groupDisplayName.value,
                    snapshot.noticeLedgerDay.value,
                    snapshot.reservedSubmissionCount.value.toString(),
                    snapshot.recordedSentSubmissionCount.value.toString(),
                    snapshot.lastOperatorLabel.value,
                ).joinToString("\t")
            }
        Files.writeString(path, body, StandardCharsets.UTF_8)
    }
}

private data class LedgerKey(
    val proofAccount: String,
    val groupId: String,
    val day: String,
)

private fun key(
    proofAccountLabel: AccountLabel,
    group: GroupMembership,
    noticeLedgerDay: NoticeLedgerDay,
): LedgerKey = key(proofAccountLabel, group.groupId, noticeLedgerDay)

private fun key(
    proofAccountLabel: AccountLabel,
    groupId: GroupId,
    noticeLedgerDay: NoticeLedgerDay,
): LedgerKey = LedgerKey(proofAccountLabel.value, groupId.value, noticeLedgerDay.value)

private fun emptySnapshot(
    proofAccountLabel: AccountLabel,
    operatorLabel: OperatorLabel,
    group: GroupMembership,
    noticeLedgerDay: NoticeLedgerDay,
): NoticeSubmissionLedgerSnapshot = NoticeSubmissionLedgerSnapshot(
    proofAccountLabel = proofAccountLabel,
    groupId = group.groupId,
    groupDisplayName = group.displayName,
    noticeLedgerDay = noticeLedgerDay,
    reservedSubmissionCount = NoticeSubmissionCount.ZERO,
    recordedSentSubmissionCount = NoticeSubmissionCount.ZERO,
    lastOperatorLabel = operatorLabel,
)

private fun NoticeSubmissionCount.minusFloorZero(other: NoticeSubmissionCount): NoticeSubmissionCount =
    NoticeSubmissionCount((value - other.value).coerceAtLeast(0L))

private fun NoticeSubmissionProjection.containsAll(groups: List<GroupMembership>): Boolean {
    val projectionIds = selectedGroups.mapTo(hashSetOf()) { it.groupId }
    return groups.all { it.groupId in projectionIds }
}
