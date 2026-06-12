package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessProofStatus
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.testing.FakeAvatarPort
import org.gem.core.testing.defaultSession

class AvatarReadinessServiceTest {
    @Test
    fun `ensureReady returns fake port success proof unchanged`() {
        val proof = AvatarReadinessProof.success()
        val port = FakeAvatarPort(AvatarReadinessResult.Success(proof))
        val session = defaultSession()

        val result = AvatarReadinessService(port).ensureReady(session)

        assertEquals(proof, assertIs<AvatarReadinessResult.Success>(result).proof)
        assertEquals(listOf(session), port.sessions)
    }

    @Test
    fun `ensureReady returns fake port failure proof and failure unchanged`() {
        val proof = AvatarReadinessProof.notRun(AvatarReadinessProofStatus.RUNTIME_GAP)
        val failure = CoreFailure(CoreFailureReason.AVATAR_READINESS_FAILED, "avatar runtime unavailable")
        val port = FakeAvatarPort(AvatarReadinessResult.Failure(proof, failure))
        val session = defaultSession()

        val result = AvatarReadinessService(port).ensureReady(session)

        val failed = assertIs<AvatarReadinessResult.Failure>(result)
        assertEquals(proof, failed.proof)
        assertEquals(failure, failed.failure)
        assertEquals(listOf(session), port.sessions)
    }

    @Test
    fun `avatar readiness proof status report values match contract`() {
        assertEquals("passed", AvatarReadinessProofStatus.PASSED.reportValue)
        assertEquals("blocked", AvatarReadinessProofStatus.BLOCKED.reportValue)
        assertEquals("transport_gap", AvatarReadinessProofStatus.TRANSPORT_GAP.reportValue)
        assertEquals("runtime_gap", AvatarReadinessProofStatus.RUNTIME_GAP.reportValue)
        assertEquals("proof_gap", AvatarReadinessProofStatus.PROOF_GAP.reportValue)
        assertEquals("not_applicable", AvatarReadinessProofStatus.NOT_APPLICABLE.reportValue)
        assertEquals("not_run", AvatarReadinessProofStatus.NOT_RUN.reportValue)
    }
}
