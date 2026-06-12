package org.hostess.ui.controllers

import org.hostess.core.domain.GroupSendState
import org.hostess.ui.testing.FakeGroupFixtures
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.testing.FakeNoticeRecorder
import org.hostess.ui.text.HostessTextKey
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
        assertEquals(HostessTextKey.BlankStatus, base.state.sendFooterState.statusTextKey)
        assertEquals(
            listOf(
                HostessTextKey.MissingSubject,
                HostessTextKey.MissingBody,
                HostessTextKey.MissingGroups,
            ),
            base.state.sendFooterState.missingRequirementKeys,
        )
        assertFalse(withSubject.state.sendFooterState.enabled)
        assertFalse(withBody.state.sendFooterState.enabled)
        assertTrue(ready.state.sendFooterState.enabled)
        assertEquals(HostessTextKey.Ready, ready.state.sendFooterState.statusTextKey)
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
    fun partialDispatchFailureIsStoredWithoutArchiveClaim() {
        val recorder = FakeNoticeRecorder(listOf(GroupSendState.SENT, GroupSendState.FAILED))
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
        assertEquals("m!nx: failed", afterSend.state.sendFooterState.detailText)
    }

    private fun readyFixture(
        recorder: FakeNoticeRecorder = FakeNoticeRecorder(),
    ): ReadySendFixture {
        val runtime = FakeHostessUiRuntime.ready(
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
        val runtime: org.hostess.ui.runtime.HostessUiRuntime,
        val session: org.hostess.core.domain.HostessSession,
        val groupController: GroupTargetController,
        val selectedAttachment: org.hostess.ui.state.SelectedAttachmentUiState,
        val noticeController: NoticeComposerController,
    )
}
