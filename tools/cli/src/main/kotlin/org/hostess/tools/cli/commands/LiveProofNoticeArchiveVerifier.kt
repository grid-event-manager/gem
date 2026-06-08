package org.hostess.tools.cli.commands

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupNoticeArchiveEntry
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.services.GroupDirectoryService

internal class LiveProofNoticeArchiveVerifier(
    private val groupDirectoryService: GroupDirectoryService,
) {
    fun read(
        session: HostessSession,
        targetSet: GroupTargetSet,
        expectedSubject: String? = null,
        requireAttachment: Boolean = false,
    ): LiveProofNoticeArchiveOutcome {
        val subjectToMatch = expectedSubject?.takeIf(String::isNotEmpty)
        return if (subjectToMatch == null) {
            readReadableArchive(session, targetSet)
        } else {
            readSubjectMatchedArchive(session, targetSet, subjectToMatch, requireAttachment)
        }
    }

    private fun readReadableArchive(
        session: HostessSession,
        targetSet: GroupTargetSet,
    ): LiveProofNoticeArchiveOutcome {
        val steps = mutableListOf<LiveProofStep>()
        var totalEntries = 0
        var firstFailureStatus: String? = null
        var firstFailureReason: String? = null

        for (group in targetSet.selectedGroups) {
            when (val result = groupDirectoryService.noticeArchive(session, group)) {
                is GroupNoticeArchiveResult.Success -> {
                    totalEntries += result.entries.size
                    steps += readableStep(group, result.entries.size)
                }
                is GroupNoticeArchiveResult.Failure -> {
                    val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                    val status = LiveProofStatusClassifier.classify(detail)
                    firstFailureStatus = firstFailureStatus ?: status
                    firstFailureReason = firstFailureReason ?: detail
                    steps += LiveProofStep(
                        "notice-archive",
                        status,
                        "target=${group.displayName.value}; $detail",
                    )
                }
            }
        }

        val passed = firstFailureStatus == null
        val status = firstFailureStatus ?: "passed"
        val statusFields = mapOf(
            "noticeArchiveStatus" to status,
            "noticeArchiveTargetCount" to targetSet.selectedCount.toString(),
            "noticeArchiveMatchedTargetCount" to "0",
        )
        val aggregate = aggregateStep(targetSet.selectedCount, matchedTargets = 0, totalEntries, subjectToMatch = null)
        return LiveProofNoticeArchiveOutcome(
            passed = passed,
            statusFields = statusFields,
            steps = steps + aggregate,
            failureReason = firstFailureReason,
        )
    }

    private fun readSubjectMatchedArchive(
        session: HostessSession,
        targetSet: GroupTargetSet,
        expectedSubject: String,
        requireAttachment: Boolean,
    ): LiveProofNoticeArchiveOutcome {
        val states = targetSet.selectedGroups.map { ArchiveGroupState(it) }
        repeat(ARCHIVE_SUBJECT_MATCH_ROUNDS) {
            states.filterNot(ArchiveGroupState::matched).forEach { state ->
                state.attempts += 1
                when (val result = groupDirectoryService.noticeArchive(session, state.group)) {
                    is GroupNoticeArchiveResult.Success -> {
                        state.result = result
                        state.match = subjectMatch(result.entries, expectedSubject, requireAttachment)
                    }
                    is GroupNoticeArchiveResult.Failure -> state.result = result
                }
            }
            if (states.all(ArchiveGroupState::matched)) {
                return@repeat
            }
        }

        val steps = mutableListOf<LiveProofStep>()
        var matchedTargets = 0
        var totalEntries = 0
        var firstFailureStatus: String? = null
        var firstFailureReason: String? = null

        states.forEach { state ->
            when (val result = state.result) {
                is GroupNoticeArchiveResult.Success -> {
                    totalEntries += result.entries.size
                    val match = state.match ?: subjectMatch(result.entries, expectedSubject, requireAttachment)
                    if (match.passed) {
                        matchedTargets += 1
                        steps += matchedStep(state.group, result.entries.size, requireAttachment, state.attempts)
                    } else {
                        val detail = withAttempts(match.detail, state.attempts)
                        firstFailureStatus = firstFailureStatus ?: "proof_gap"
                        firstFailureReason = firstFailureReason ?: detail
                        steps += LiveProofStep("notice-archive", "proof_gap", "target=${state.group.displayName.value}; $detail")
                    }
                }
                is GroupNoticeArchiveResult.Failure -> {
                    val detail = result.failure.redactedMessage ?: result.failure.reason.name.lowercase()
                    val retryDetail = withAttempts(detail, state.attempts)
                    val status = LiveProofStatusClassifier.classify(detail)
                    firstFailureStatus = firstFailureStatus ?: status
                    firstFailureReason = firstFailureReason ?: retryDetail
                    steps += LiveProofStep(
                        "notice-archive",
                        status,
                        "target=${state.group.displayName.value}; $retryDetail",
                    )
                }
                null -> {
                    val detail = "notice archive proof_gap not_run"
                    firstFailureStatus = firstFailureStatus ?: "proof_gap"
                    firstFailureReason = firstFailureReason ?: detail
                    steps += LiveProofStep("notice-archive", "proof_gap", "target=${state.group.displayName.value}; $detail")
                }
            }
        }

        val passed = firstFailureStatus == null
        val status = firstFailureStatus ?: "passed"
        val statusFields = mapOf(
            "noticeArchiveStatus" to status,
            "noticeArchiveTargetCount" to targetSet.selectedCount.toString(),
            "noticeArchiveMatchedTargetCount" to matchedTargets.toString(),
        )
        val aggregate = aggregateStep(targetSet.selectedCount, matchedTargets, totalEntries, expectedSubject)
        return LiveProofNoticeArchiveOutcome(
            passed = passed,
            statusFields = statusFields,
            steps = steps + aggregate,
            failureReason = firstFailureReason,
        )
    }

    private fun readableStep(group: GroupMembership, entries: Int): LiveProofStep =
        LiveProofStep.passed(
            "notice-archive",
            "target=${group.displayName.value}; entries=$entries; bodyEcho=not_run",
        )

    private fun matchedStep(
        group: GroupMembership,
        entries: Int,
        requireAttachment: Boolean,
        attempts: Int,
    ): LiveProofStep =
        LiveProofStep.passed(
            "notice-archive",
            "target=${group.displayName.value}; entries=$entries; subjectMatch=passed; " +
                "attachmentEcho=${if (requireAttachment) "passed" else "not_required"}; " +
                "bodyEcho=not_run${attemptSuffix(attempts)}",
        )

    private fun aggregateStep(
        targetCount: Int,
        matchedTargets: Int,
        totalEntries: Int,
        subjectToMatch: String?,
    ): LiveProofStep =
        if (subjectToMatch == null) {
            LiveProofStep.passed(
                "notice-archive",
                "targets=$targetCount; readable=$targetCount; entries=$totalEntries; bodyEcho=not_run",
            )
        } else {
            val state = if (matchedTargets == targetCount) "passed" else "proof_gap"
            LiveProofStep(
                "notice-archive",
                state,
                "targets=$targetCount; matched=$matchedTargets; entries=$totalEntries; bodyEcho=not_run",
            )
        }

    private fun subjectMatch(
        entries: List<GroupNoticeArchiveEntry>,
        expectedSubject: String,
        requireAttachment: Boolean,
    ): ArchiveSubjectMatch {
        val sameSubject = entries.filter { it.subject == expectedSubject }
        if (sameSubject.isEmpty()) {
            return ArchiveSubjectMatch(false, "entries=${entries.size}; subjectMatch=missing; bodyEcho=not_run")
        }
        if (requireAttachment && sameSubject.none { it.hasAttachment }) {
            return ArchiveSubjectMatch(
                false,
                "entries=${entries.size}; subjectMatch=passed; attachmentEcho=missing; bodyEcho=not_run",
            )
        }
        return ArchiveSubjectMatch(true, "subjectMatch=passed")
    }

    private fun withAttempts(detail: String, attempts: Int): String =
        "$detail${attemptSuffix(attempts)}"

    private fun attemptSuffix(attempts: Int): String =
        if (attempts > 1) "; attempts=$attempts" else ""

    private companion object {
        const val ARCHIVE_SUBJECT_MATCH_ROUNDS = 4
    }
}

internal data class LiveProofNoticeArchiveOutcome(
    val passed: Boolean,
    val statusFields: Map<String, String>,
    val steps: List<LiveProofStep>,
    val failureReason: String?,
)

private data class ArchiveSubjectMatch(
    val passed: Boolean,
    val detail: String,
)

private class ArchiveGroupState(
    val group: GroupMembership,
) {
    var attempts: Int = 0
    var result: GroupNoticeArchiveResult? = null
    var match: ArchiveSubjectMatch? = null
    val matched: Boolean
        get() = match?.passed == true
}
