package org.hostess.protocol.libomv.transport

import java.time.Duration
import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.llsd.LlsdValue
import org.hostess.protocol.libomv.llsd.LlsdXml
import org.hostess.protocol.libomv.llsd.asBoolean
import org.hostess.protocol.libomv.llsd.asLong
import org.hostess.protocol.libomv.llsd.asString

internal interface EventQueueGetSource {
    fun seed(seedCapability: String): EventQueueGetResult
    fun pollAgentGroupDataUpdate(eventQueueUrl: String): EventQueueGetResult
}

internal class EventQueueGetClient(
    private val httpClient: ProtocolHttpClient,
    private val maxPolls: Int = DEFAULT_MAX_POLLS,
) : EventQueueGetSource {
    override fun seed(seedCapability: String): EventQueueGetResult {
        if (seedCapability.isBlank()) {
            return EventQueueGetResult.TransportGap
        }
        val response = execute(request(seedCapability, seedBody(), Duration.ofSeconds(30)))
            ?: return EventQueueGetResult.TransportGap
        if (response.statusCode !in 200..299) {
            return EventQueueGetResult.TransportGap
        }
        val fields = LlsdXml.parseMap(response.body) ?: return EventQueueGetResult.MappingGap
        val eventQueueUrl = fields[EVENT_QUEUE_GET]?.asString()?.takeIf(String::isNotBlank)
            ?: return EventQueueGetResult.TransportGap
        return EventQueueGetResult.Ready(eventQueueUrl)
    }

    override fun pollAgentGroupDataUpdate(eventQueueUrl: String): EventQueueGetResult {
        if (eventQueueUrl.isBlank()) {
            return EventQueueGetResult.TransportGap
        }
        var ack: Long? = null
        repeat(maxPolls) {
            val response = execute(request(eventQueueUrl, pollBody(ack), Duration.ofSeconds(60)))
                ?: return EventQueueGetResult.TransportGap
            if (response.statusCode !in 200..299) {
                return EventQueueGetResult.TransportGap
            }
            val fields = LlsdXml.parseMap(response.body) ?: return EventQueueGetResult.MappingGap
            val events = fields["events"] as? LlsdValue.ArrayValue
            if (events == null) {
                if (fields.containsKey("ack")) {
                    ack = fields["ack"]?.asLong() ?: return EventQueueGetResult.MappingGap
                    return@repeat
                }
                return EventQueueGetResult.MappingGap
            }
            ack = fields["id"]?.asLong() ?: return EventQueueGetResult.MappingGap
            if (events.values.isEmpty()) {
                return@repeat
            }
            return agentGroupDataUpdate(events)
        }
        return EventQueueGetResult.TimedOut
    }

    private fun agentGroupDataUpdate(events: LlsdValue.ArrayValue): EventQueueGetResult {
        for (event in events.values) {
            val eventFields = (event as? LlsdValue.MapValue)?.values ?: return EventQueueGetResult.MappingGap
            val message = eventFields["message"]?.asString() ?: return EventQueueGetResult.MappingGap
            if (message != AGENT_GROUP_DATA_UPDATE) {
                return EventQueueGetResult.MappingGap
            }
            val body = (eventFields["body"] as? LlsdValue.MapValue)?.values
                ?: return EventQueueGetResult.MappingGap
            return parseAgentGroupDataUpdate(body)
        }
        return EventQueueGetResult.TimedOut
    }

    private fun parseAgentGroupDataUpdate(body: Map<String, LlsdValue>): EventQueueGetResult {
        if (!hasAgentData(body["AgentData"])) {
            return EventQueueGetResult.MappingGap
        }
        val groups = (body["GroupData"] as? LlsdValue.ArrayValue)?.values
            ?: return EventQueueGetResult.MappingGap
        val newGroupData = body["NewGroupData"]
        val newGroups = when (newGroupData) {
            null -> null
            is LlsdValue.ArrayValue -> newGroupData.values
            else -> return EventQueueGetResult.MappingGap
        }
        if (newGroups != null && newGroups.size != groups.size) {
            return EventQueueGetResult.MappingGap
        }
        return EventQueueGetResult.AgentGroupDataUpdate(
            groups = groups.mapIndexed { index, value ->
                val fields = (value as? LlsdValue.MapValue)?.values
                    ?: return EventQueueGetResult.MappingGap
                LibomvGroupSnapshot(
                    groupId = fields["GroupID"]?.asString() ?: return EventQueueGetResult.MappingGap,
                    displayName = fields["GroupName"]?.asString() ?: return EventQueueGetResult.MappingGap,
                    powers = fields["GroupPowers"]?.asLong() ?: return EventQueueGetResult.MappingGap,
                    acceptsNotices = fields["AcceptNotices"]?.asBoolean()
                        ?: return EventQueueGetResult.MappingGap,
                ).also {
                    if (newGroups != null) {
                        val newGroupFields = (newGroups[index] as? LlsdValue.MapValue)?.values
                            ?: return EventQueueGetResult.MappingGap
                        newGroupFields["ListInProfile"]?.asBoolean()
                            ?: return EventQueueGetResult.MappingGap
                    }
                }
            },
        )
    }

    private fun hasAgentData(value: LlsdValue?): Boolean {
        val agents = (value as? LlsdValue.ArrayValue)?.values ?: return false
        val fields = (agents.firstOrNull() as? LlsdValue.MapValue)?.values ?: return false
        return !fields["AgentID"]?.asString().isNullOrBlank()
    }

    private fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse? = try {
        httpClient.execute(request)
    } catch (ex: ProtocolHttpException) {
        null
    }

    private fun request(url: String, body: String, timeout: Duration): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = url,
        headers = mapOf("Content-Type" to LLSD_XML),
        body = ProtocolHttpBody.TextBody(body, LLSD_XML),
        timeout = timeout,
    )

    private fun seedBody(): String =
        "<llsd><array><string>$EVENT_QUEUE_GET</string></array></llsd>"

    private fun pollBody(ack: Long?): String = buildString {
        append("<llsd><map><key>ack</key>")
        if (ack == null) {
            append("<undef/>")
        } else {
            append("<integer>").append(ack).append("</integer>")
        }
        append("<key>done</key><boolean>false</boolean></map></llsd>")
    }

    private companion object {
        const val DEFAULT_MAX_POLLS = 3
        const val LLSD_XML = "application/llsd+xml"
        const val EVENT_QUEUE_GET = "EventQueueGet"
        const val AGENT_GROUP_DATA_UPDATE = "AgentGroupDataUpdate"
    }
}

internal sealed interface EventQueueGetResult {
    data class Ready(val eventQueueUrl: String) : EventQueueGetResult
    data object TimedOut : EventQueueGetResult
    data object TransportGap : EventQueueGetResult
    data object MappingGap : EventQueueGetResult
    data class AgentGroupDataUpdate(val groups: List<LibomvGroupSnapshot>) : EventQueueGetResult
}
