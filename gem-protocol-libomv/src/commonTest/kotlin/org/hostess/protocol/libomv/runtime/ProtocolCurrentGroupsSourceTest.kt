package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.LibomvMapping
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequester
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequestResult
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import org.hostess.protocol.libomv.transport.EventQueueGetResult
import org.hostess.protocol.libomv.transport.EventQueueGetSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProtocolCurrentGroupsSourceTest {
    @Test
    fun `current groups resolves event queue url sends request then polls group event`() {
        val groups = listOf(group())
        val capabilityProvider = RecordingCapabilityProvider(CapabilityUrlResult.Ready(EVENT_URL))
        val eventSource = RecordingEventQueueSource(
            pollResult = EventQueueGetResult.AgentGroupDataUpdate(groups),
        )
        val requester = RecordingRequester(AgentDataUpdateRequestResult.Sent)
        val source = ProtocolCurrentGroupsSource(capabilityProvider, eventSource, requester)

        val result = assertIs<CurrentGroupsFetchResult.Success>(source.currentGroups(identity()))

        assertEquals(groups, result.groups)
        assertEquals(CapabilityName.EVENT_QUEUE_GET, capabilityProvider.requestedName)
        assertEquals(identity(), capabilityProvider.capturedIdentity)
        assertEquals(EVENT_URL, eventSource.polledUrl)
        assertEquals(identity(), requester.capturedIdentity)
    }

    @Test
    fun `capability transport gap stops before packet request`() {
        val capabilityProvider = RecordingCapabilityProvider(
            CapabilityUrlResult.TransportGap("capability seed unavailable: http_status=503"),
        )
        val eventSource = RecordingEventQueueSource()
        val requester = RecordingRequester(AgentDataUpdateRequestResult.Sent)
        val source = ProtocolCurrentGroupsSource(capabilityProvider, eventSource, requester)

        val failure = assertIs<CurrentGroupsFetchResult.Failure>(source.currentGroups(identity()))

        assertEquals(CurrentGroupsFailureStatus.TRANSPORT_GAP, failure.status)
        assertEquals("capability seed unavailable: http_status=503", failure.redactedMessage)
        assertNull(requester.capturedIdentity)
        assertNull(eventSource.polledUrl)
    }

    @Test
    fun `packet request failure is a packet gap`() {
        val capabilityProvider = RecordingCapabilityProvider(CapabilityUrlResult.Ready(EVENT_URL))
        val eventSource = RecordingEventQueueSource()
        val requester = RecordingRequester(
            AgentDataUpdateRequestResult.Failed("bounded simulator send failed"),
        )
        val source = ProtocolCurrentGroupsSource(capabilityProvider, eventSource, requester)

        val failure = assertIs<CurrentGroupsFetchResult.Failure>(source.currentGroups(identity()))

        assertEquals(CurrentGroupsFailureStatus.PACKET_GAP, failure.status)
        assertEquals("current groups transport packet failed: bounded simulator send failed", failure.redactedMessage)
        assertNull(eventSource.polledUrl)
    }

    @Test
    fun `poll timeout is a proof gap`() {
        val capabilityProvider = RecordingCapabilityProvider(CapabilityUrlResult.Ready(EVENT_URL))
        val eventSource = RecordingEventQueueSource(
            pollResult = EventQueueGetResult.TimedOut,
        )
        val requester = RecordingRequester(AgentDataUpdateRequestResult.Sent)
        val source = ProtocolCurrentGroupsSource(capabilityProvider, eventSource, requester)

        val failure = assertIs<CurrentGroupsFetchResult.Failure>(source.currentGroups(identity()))

        assertEquals(CurrentGroupsFailureStatus.PROOF_GAP, failure.status)
        assertEquals("current groups event unavailable", failure.redactedMessage)
    }

    @Test
    fun `poll mapping gap is a proof gap with invalid event message`() {
        val capabilityProvider = RecordingCapabilityProvider(CapabilityUrlResult.Ready(EVENT_URL))
        val eventSource = RecordingEventQueueSource(
            pollResult = EventQueueGetResult.MappingGap("current groups event invalid: malformed group data"),
        )
        val requester = RecordingRequester(AgentDataUpdateRequestResult.Sent)
        val source = ProtocolCurrentGroupsSource(capabilityProvider, eventSource, requester)

        val failure = assertIs<CurrentGroupsFetchResult.Failure>(source.currentGroups(identity()))

        assertEquals(CurrentGroupsFailureStatus.PROOF_GAP, failure.status)
        assertEquals("current groups event invalid: malformed group data", failure.redactedMessage)
    }

    private class RecordingEventQueueSource(
        private val pollResult: EventQueueGetResult = EventQueueGetResult.TimedOut,
    ) : EventQueueGetSource {
        var polledUrl: CapabilityUrl? = null

        override fun pollAgentGroupDataUpdate(eventQueueUrl: CapabilityUrl): EventQueueGetResult {
            polledUrl = eventQueueUrl
            return pollResult
        }
    }

    private class RecordingCapabilityProvider(
        private val result: CapabilityUrlResult,
    ) : CapabilityUrlProvider {
        var capturedIdentity: LibomvSessionIdentity? = null
        var requestedName: CapabilityName? = null

        override fun requireUrl(
            identity: LibomvSessionIdentity,
            name: CapabilityName,
        ): CapabilityUrlResult {
            capturedIdentity = identity
            requestedName = name
            return result
        }
    }

    private class RecordingRequester(
        private val result: AgentDataUpdateRequestResult,
    ) : AgentDataUpdateRequester {
        var capturedIdentity: LibomvSessionIdentity? = null

        override fun send(identity: LibomvSessionIdentity): AgentDataUpdateRequestResult {
            capturedIdentity = identity
            return result
        }
    }

    private fun group(): LibomvGroupSnapshot = LibomvGroupSnapshot(
        groupId = "33333333-3333-3333-3333-333333333333",
        displayName = "Music Room",
        powers = LibomvMapping.SEND_NOTICES_POWER,
        acceptsNotices = true,
    )

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = "11111111-1111-1111-1111-111111111111",
        sessionId = "22222222-2222-2222-2222-222222222222",
        seedCapability = SEED_URL,
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private companion object {
        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"

        val SEED_URL = secureUrl("caps.example", "/seed")
        val EVENT_URL = CapabilityUrl(secureUrl("caps.example", "/event"))
    }
}
