package org.hostess.core.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NoticeDraftTest {
    @Test
    fun `validates nonblank subject and selected target requirement`() {
        val emptyTargets = GroupTargetSet.from(listOf(group("music", "Music Room")))
        val invalid = assertIs<NoticeDraftValidation.Invalid>(
            NoticeDraft(
                subject = " ",
                message = "Tonight at 8",
                targetSet = emptyTargets,
            ).validateForSend(),
        )

        assertContains(invalid.reasons, NoticeDraftInvalidReason.BLANK_SUBJECT)
        assertContains(invalid.reasons, NoticeDraftInvalidReason.EMPTY_TARGET_SET)
    }

    @Test
    fun `validates one attachment invariant`() {
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
            attachments = listOf(
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
                UploadTextureAttachment("poster.png", "sha256:abc"),
            ),
        )

        val invalid = assertIs<NoticeDraftValidation.Invalid>(draft.validateForSend())

        assertEquals(setOf(NoticeDraftInvalidReason.TOO_MANY_ATTACHMENTS), invalid.reasons)
    }

    @Test
    fun `creates send plan only from valid draft`() {
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        ).withAttachment(
            ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
        )

        val plan = NoticeSendPlan(draft)

        assertEquals(1, plan.targetGroups.size)
        assertEquals(NoticeDraftValidation.Valid, draft.validateForSend())
    }

    @Test
    fun `rejects invalid draft for send planning`() {
        val draft = NoticeDraft(
            subject = "",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        assertFailsWith<IllegalArgumentException> {
            NoticeSendPlan(draft)
        }
    }

    private fun selectedTargets(): GroupTargetSet {
        val targets = GroupTargetSet.from(listOf(group("music", "Music Room")))
        return assertIs<TargetSelectionResult.Changed>(targets.add(GroupDisplayName("Music Room"))).targetSet
    }

    private fun group(id: String, displayName: String): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = true,
        acceptsNotices = null,
    )
}
