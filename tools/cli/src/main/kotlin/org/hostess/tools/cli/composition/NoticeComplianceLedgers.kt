package org.hostess.tools.cli.composition

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.ports.NoticeComplianceLedgerPort

internal class InMemoryNoticeComplianceLedgerPort : NoticeComplianceLedgerPort {
    private val snapshots = linkedMapOf<LedgerKey, NoticeDeliveryLedgerSnapshot>()

    override fun snapshot(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        NoticeComplianceLedgerResult.Success(currentSnapshot(operatorLabel, deliveryDay))

    override fun reserve(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        projected: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
        val current = currentSnapshot(operatorLabel, deliveryDay)
        val updated = current.copy(
            reservedDeliveryCount = current.reservedDeliveryCount.plus(projected),
        )
        snapshots[LedgerKey(operatorLabel.value, deliveryDay.value)] = updated
        return NoticeComplianceLedgerResult.Success(updated)
    }

    override fun recordSendResult(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        reservedProjection: NoticeDeliveryCount,
        delivered: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
        val current = currentSnapshot(operatorLabel, deliveryDay)
        val updated = current.copy(
            reservedDeliveryCount = current.reservedDeliveryCount.minusFloorZero(reservedProjection),
            recordedSentDeliveryCount = current.recordedSentDeliveryCount.plus(delivered),
        )
        snapshots[LedgerKey(operatorLabel.value, deliveryDay.value)] = updated
        return NoticeComplianceLedgerResult.Success(updated)
    }

    private fun currentSnapshot(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
    ): NoticeDeliveryLedgerSnapshot =
        snapshots[LedgerKey(operatorLabel.value, deliveryDay.value)] ?: emptySnapshot(operatorLabel, deliveryDay)
}

internal class UnavailableNoticeComplianceLedgerPort : NoticeComplianceLedgerPort {
    override fun snapshot(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")

    override fun reserve(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        projected: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")

    override fun recordSendResult(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        reservedProjection: NoticeDeliveryCount,
        delivered: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
}

internal class FileNoticeComplianceLedgerPort(
    private val path: Path,
) : NoticeComplianceLedgerPort {
    override fun snapshot(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        try {
            val snapshot = readSnapshots()[LedgerKey(operatorLabel.value, deliveryDay.value)]
                ?: emptySnapshot(operatorLabel, deliveryDay)
            NoticeComplianceLedgerResult.Success(snapshot)
        } catch (_: Exception) {
            NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }

    override fun reserve(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        projected: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        mutate(operatorLabel, deliveryDay) { snapshots, key ->
            val current = snapshots.getOrPut(key) { emptySnapshot(operatorLabel, deliveryDay) }
            val updated = current.copy(
                reservedDeliveryCount = current.reservedDeliveryCount.plus(projected),
            )
            snapshots[key] = updated
            updated
        }

    override fun recordSendResult(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        reservedProjection: NoticeDeliveryCount,
        delivered: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        mutate(operatorLabel, deliveryDay) { snapshots, key ->
            val current = snapshots.getOrPut(key) { emptySnapshot(operatorLabel, deliveryDay) }
            val updated = current.copy(
                reservedDeliveryCount = current.reservedDeliveryCount.minusFloorZero(reservedProjection),
                recordedSentDeliveryCount = current.recordedSentDeliveryCount.plus(delivered),
            )
            snapshots[key] = updated
            updated
        }

    private fun mutate(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        action: (MutableMap<LedgerKey, NoticeDeliveryLedgerSnapshot>, LedgerKey) -> NoticeDeliveryLedgerSnapshot,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
        try {
            val snapshots = readSnapshots().toMutableMap()
            val key = LedgerKey(operatorLabel.value, deliveryDay.value)
            val result = action(snapshots, key)
            writeSnapshots(snapshots)
            NoticeComplianceLedgerResult.Success(result)
        } catch (_: Exception) {
            NoticeComplianceLedgerResult.Failure("notice_ledger_unavailable")
        }

    private fun readSnapshots(): Map<LedgerKey, NoticeDeliveryLedgerSnapshot> {
        if (!path.exists()) {
            return emptyMap()
        }

        return Files.readAllLines(path, StandardCharsets.UTF_8)
            .filter(String::isNotBlank)
            .associate { row ->
                val parts = row.split('\t')
                require(parts.size == 4) { "malformed notice ledger row" }
                val operatorLabel = OperatorLabel(parts[0])
                val deliveryDay = NoticeDeliveryDay(parts[1])
                val snapshot = NoticeDeliveryLedgerSnapshot(
                    operatorLabel = operatorLabel,
                    deliveryDay = deliveryDay,
                    reservedDeliveryCount = NoticeDeliveryCount(parts[2].toLong()),
                    recordedSentDeliveryCount = NoticeDeliveryCount(parts[3].toLong()),
                )
                LedgerKey(operatorLabel.value, deliveryDay.value) to snapshot
            }
    }

    private fun writeSnapshots(snapshots: Map<LedgerKey, NoticeDeliveryLedgerSnapshot>) {
        path.parent?.let(Files::createDirectories)
        val body = snapshots.entries
            .sortedWith(compareBy({ it.key.operator }, { it.key.day }))
            .joinToString(separator = "\n", postfix = "\n") { (_, snapshot) ->
                listOf(
                    snapshot.operatorLabel.value,
                    snapshot.deliveryDay.value,
                    snapshot.reservedDeliveryCount.value.toString(),
                    snapshot.recordedSentDeliveryCount.value.toString(),
                ).joinToString("\t")
            }
        Files.writeString(path, body, StandardCharsets.UTF_8)
    }
}

private data class LedgerKey(
    val operator: String,
    val day: String,
)

private fun emptySnapshot(
    operatorLabel: OperatorLabel,
    deliveryDay: NoticeDeliveryDay,
): NoticeDeliveryLedgerSnapshot = NoticeDeliveryLedgerSnapshot(
    operatorLabel = operatorLabel,
    deliveryDay = deliveryDay,
    reservedDeliveryCount = NoticeDeliveryCount.ZERO,
    recordedSentDeliveryCount = NoticeDeliveryCount.ZERO,
)

private fun NoticeDeliveryCount.minusFloorZero(other: NoticeDeliveryCount): NoticeDeliveryCount =
    NoticeDeliveryCount((value - other.value).coerceAtLeast(0L))
