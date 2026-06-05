package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProtocolLibomvModuleTest {
    @Test
    fun `live runtime exposes one adapter set with shared session holder`() {
        val runtime = ProtocolLibomvModule.liveRuntime()

        assertTrue(runtime.protocolAvailable)
        assertTrue(runtime.clientSession.isProtocolAvailable())
        assertSame(runtime.clientSession, (runtime.sessionPort as LibomvSessionAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.groupPort as LibomvGroupAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.inventoryPort as LibomvInventoryAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.noticePort as LibomvNoticeAdapter).clientSession)
        assertTrue(runtime.loadState.adapterLoad)
        assertTrue(runtime.loadState.runtimeLoad)
        assertTrue(runtime.loadState.transportLoad)
    }

    @Test
    fun `unavailable runtime composition still fails closed`() {
        val clientSession = LibomvClientSession.unavailable()
        val login = LibomvSessionAdapter(clientSession).login(loginRequest())

        assertFalse(clientSession.isProtocolAvailable())
        assertIs<SessionLoginResult.Failure>(login)
        assertEquals(CoreFailureReason.LOGIN_FAILED, login.failure.reason)
        assertEquals("protocol bootstrap unavailable", login.failure.redactedMessage)
    }

    @Test
    fun `live runtime unresolved login and unimplemented methods still fail closed`() {
        val runtime = ProtocolLibomvModule.liveRuntime()

        val login = runtime.sessionPort.login(loginRequest())
        val groups = runtime.groupPort.currentGroups(fakeInactiveSession())

        assertIs<SessionLoginResult.Failure>(login)
        assertEquals(CoreFailureReason.LOGIN_FAILED, login.failure.reason)
        assertEquals("login secret unavailable", login.failure.redactedMessage)

        assertIs<GroupListResult.Failure>(groups)
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, groups.failure.reason)
        assertEquals("protocol session inactive", groups.failure.redactedMessage)
    }

    @Test
    fun `live runtime notice adapter reaches protocol runtime source`() {
        val runtime = ProtocolLibomvModule.liveRuntime()
        val session = fakeActiveUuidSession()
        runtime.clientSession.activate(session, agentId = "11111111-1111-1111-1111-111111111111")

        val status = runtime.noticePort.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice runtime unavailable", status.detail)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("venue-proof"),
        credentialHandle = CredentialHandle("proof-handle"),
    )

    private fun fakeInactiveSession(): HostessSession = HostessSession(
        sessionId = SessionId("inactive-proof-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = false,
    )

    private fun fakeActiveUuidSession(): HostessSession = HostessSession(
        sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )

    private fun draft(): NoticeDraft = NoticeDraft(
        subject = "Gig tonight",
        message = "Doors at 8",
        targetSet = assertIs<TargetSelectionResult.Changed>(
            GroupTargetSet.from(listOf(group())).addAllSendable(),
        ).targetSet,
    )

    private fun group(): GroupMembership = GroupMembership(
        groupId = GroupId("33333333-3333-3333-3333-333333333333"),
        displayName = GroupDisplayName("Music Room"),
        canSendNotices = true,
        acceptsNotices = true,
    )
}
