package org.gem.ui.controllers

import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.ui.testing.FakeGroupFixtures
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeInventoryFixtures
import org.gem.ui.testing.FakeNoticeRecorder
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SendFooterControllerTest {
    @Test
    fun footerStaysBlankAndDisabledUntilEveryRequiredSendFieldExists() {
        val fixture = readyFixture()
        val base = fixture.noticeController

        val withSubject = base.updateSubject("Tonight")
        val withBody = withSubject.updateBody("Body with emoji \uD83C\uDFA7")
        val ready = withBody.updateTargetSet(fixture.groupController.selectAllGroupsMode().targetSet)

        assertFalse(base.state.sendFooterState.enabled)
        assertEquals(GemTextKey.BlankStatus, base.state.sendFooterState.statusTextKey)
        assertEquals(
            listOf(
                GemTextKey.MissingSubject,
                GemTextKey.MissingBody,
                GemTextKey.MissingGroups,
            ),
            base.state.sendFooterState.missingRequirementKeys,
        )
        assertFalse(withSubject.state.sendFooterState.enabled)
        assertFalse(withBody.state.sendFooterState.enabled)
        assertTrue(ready.state.sendFooterState.enabled)
        assertEquals(GemTextKey.Ready, ready.state.sendFooterState.statusTextKey)
        assertEquals(emptyList(), ready.state.sendFooterState.missingRequirementKeys)
        assertFalse(ready.state.sendFooterState.showMissingRequirements)
    }

    @Test
    fun footerRequiresAvatarReadinessAndSession() {
        val fixture = readyFixture()
        val validTargetSet = fixture.groupController.selectAllGroupsMode().targetSet
        val missingSession = NoticeComposerController(
            runtime = fixture.runtime,
            session = null,
            avatarReady = true,
        ).updateSubject("Tonight")
            .updateBody("Body")
            .updateTargetSet(validTargetSet)
        val avatarBlocked = NoticeComposerController(
            runtime = fixture.runtime,
            session = fixture.session,
            avatarReady = false,
        ).updateSubject("Tonight")
            .updateBody("Body")
            .updateTargetSet(validTargetSet)

        assertFalse(missingSession.state.sendFooterState.enabled)
        assertFalse(avatarBlocked.state.sendFooterState.enabled)
    }

    @Test
    fun sendDisabledReturnsWithoutDispatch() {
        val recorder = FakeNoticeRecorder()
        val fixture = readyFixture(recorder = recorder)
        val disabled = fixture.noticeController.updateSubject("Tonight")

        val afterSend = disabled.sendNotices()

        assertEquals(0, recorder.sendCallCount)
        assertFalse(afterSend.state.sendAttempted)
        assertTrue(afterSend.state.sendFooterState.showMissingRequirements)
    }

    @Test
    fun sendEnabledDispatchesOnceForSingleSelectedTargetAndStoresSummary() {
        val recorder = FakeNoticeRecorder()
        val fixture = readyFixture(recorder = recorder)
        val targetSet = fixture.groupController
            .selectManualGroupsMode()
            .setManualGroupSelected("Owks", true)
            .targetSet
        val ready = fixture.noticeController
            .updateSubject("Tonight")
            .updateBody("Body")
            .updateTargetSet(targetSet)

        val afterSend = ready.sendNotices()

        assertEquals(1, recorder.sendCallCount)
        assertEquals(listOf("Owks"), recorder.sentGroupDisplayNames)
        assertTrue(afterSend.state.sendAttempted)
        assertEquals(1, afterSend.state.sentGroupCount)
        assertEquals(0, afterSend.state.failedGroupCount)
        assertFalse(afterSend.state.dispatchRejected)
    }

    @Test
    fun postSendOffAppFailureIsDetectedButNotSurfacedInFooter() {
        val recorder = FakeNoticeRecorder(
            scriptedStates = listOf(GroupSendState.SENT, GroupSendState.FAILED),
            scriptedDetails = listOf(null, "notice send ack timeout after 3 attempts"),
        )
        val fixture = readyFixture(recorder = recorder)
        val targetSet = fixture.groupController.selectAllGroupsMode().targetSet
        val ready = fixture.noticeController
            .updateSubject("Tonight")
            .updateBody("Body")
            .updateTargetSet(targetSet)

        val afterSend = ready.sendNotices()

        assertEquals(2, recorder.sendCallCount)
        assertEquals(1, afterSend.state.sentGroupCount)
        assertEquals(1, afterSend.state.failedGroupCount)
        assertEquals(GemTextKey.NoticesSent, afterSend.state.sendFooterState.statusTextKey)
        assertEquals(emptyList(), afterSend.state.sendFooterState.failureDetails)
        assertFalse(afterSend.state.sendFooterState.failureDetailsExpanded)

        val expanded = afterSend.toggleSendFailureDetails()

        assertFalse(expanded.state.sendFooterState.failureDetailsExpanded)
    }

    @Test
    fun onlyAppOwnedDispatchFailuresAreStoredAsSeparateDetails() {
        val groups = listOf(
            sendableGroup("group-one", "One"),
            sendableGroup("group-two", "Two"),
            sendableGroup("group-three", "Three"),
            sendableGroup("group-four", "Four"),
        )
        val recorder = FakeNoticeRecorder(
            scriptedStates = groups.map { GroupSendState.FAILED },
            scriptedDetails = listOf(
                "notice send ack timeout after 3 attempts",
                "protocol simulator send failed",
                "notice runtime unavailable",
                "notice request invalid",
            ),
        )
        val runtime = FakeGemUiRuntime.ready(
            groups = groups,
            inventoryListing = FakeInventoryFixtures.listing(),
            noticeRecorder = recorder,
        )
        val session = FakeInventoryFixtures.session()
        val targetSet = NoticeComposerController(runtime, session, avatarReady = true)
            .refreshGroups()
            .selectAllGroupsMode()
            .targetSet
        val ready = NoticeComposerController(runtime, session, avatarReady = true)
            .updateSubject("Tonight")
            .updateBody("Body")
            .updateTargetSet(targetSet)

        val afterSend = ready.sendNotices()

        assertEquals(4, afterSend.state.failedGroupCount)
        assertEquals(GemTextKey.SomeNoticesFailed, afterSend.state.sendFooterState.statusTextKey)
        assertEquals(
            listOf("Three", "Four"),
            afterSend.state.sendFooterState.failureDetails.map { it.groupName },
        )
        assertEquals(
            listOf(
                GemTextKey.SendFailureSenderUnavailable,
                GemTextKey.SendFailureRequestInvalid,
            ),
            afterSend.state.sendFooterState.failureDetails.map { it.reasonKey },
        )
    }

    private fun readyFixture(
        recorder: FakeNoticeRecorder = FakeNoticeRecorder(),
    ): ReadySendFixture {
        val runtime = FakeGemUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            inventoryListing = FakeInventoryFixtures.listing(),
            noticeRecorder = recorder,
        )
        val session = FakeInventoryFixtures.session()
        val groupController = NoticeComposerController(runtime, session, avatarReady = true).refreshGroups()
        val selectedAttachment = InventoryBrowserController(runtime, session)
            .refreshInventory()
            .selectInventoryAsset(FakeInventoryFixtures.welcomeItemId)
            .state
            .selectedAttachment ?: error("missing selected attachment")
        return ReadySendFixture(
            runtime = runtime,
            session = session,
            groupController = groupController,
            selectedAttachment = selectedAttachment,
            noticeController = NoticeComposerController(runtime, session, avatarReady = true),
        )
    }

    private data class ReadySendFixture(
        val runtime: org.gem.ui.runtime.GemUiRuntime,
        val session: org.gem.core.domain.GemSession,
        val groupController: GroupTargetController,
        val selectedAttachment: org.gem.ui.state.SelectedAttachmentUiState,
        val noticeController: NoticeComposerController,
    )

    private fun sendableGroup(id: String, displayName: String): GroupMembership =
        GroupMembership.fromValues(
            groupId = id,
            displayName = displayName,
            canSendNotices = true,
            acceptsNotices = true,
        )
}
