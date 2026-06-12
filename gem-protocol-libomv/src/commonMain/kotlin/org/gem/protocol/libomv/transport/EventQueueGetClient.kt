package org.gem.protocol.libomv.transport

import org.gem.core.domain.GemDelay
import org.gem.core.services.SafeDiagnosticRedaction
import org.gem.protocol.libomv.LibomvGroupSnapshot
import org.gem.protocol.libomv.llsd.LlsdValue
import org.gem.protocol.libomv.llsd.LlsdXml
import org.gem.protocol.libomv.llsd.asBoolean
import org.gem.protocol.libomv.llsd.asLong
import org.gem.protocol.libomv.llsd.asString

internal interface EventQueueGetSource {
    fun pollAgentGroupDataUpdate(eventQueueUrl: CapabilityUrl): EventQueueGetResult
}

internal class EventQueueGetClient(
    private val httpClient: ProtocolHttpClient,
    private val maxPolls: Int = DEFAULT_MAX_POLLS,
) : EventQueueGetSource {
    override fun pollAgentGroupDataUpdate(eventQueueUrl: CapabilityUrl): EventQueueGetResult {
        var ack: Long? = null
        repeat(maxPolls) {
            val response = when (val executed = execute(request(eventQueueUrl.value, pollBody(ack), GemDelay.ofSeconds(60)))) {
                is EventQueueHttpResult.Failed -> return EventQueueGetResult.TransportGap(executed.redactedMessage)
                is EventQueueHttpResult.Success -> executed.response
            }
            if (response.statusCode !in 200..299) {
                return transportGap("http_status=${response.statusCode}; ${responseDiagnostic(response)}")
            }
            val fields = LlsdXml.parseMap(response.body) ?: return mappingGap("poll response invalid; ${bodyDiagnostic(response.body)}")
            val events = fields["events"] as? LlsdValue.ArrayValue
            if (events == null) {
                if (fields.containsKey("ack")) {
                    ack = fields["ack"]?.asLong() ?: return mappingGap("poll ack invalid")
                    return@repeat
                }
                return mappingGap("poll events absent")
            }
            ack = fields["id"]?.asLong() ?: return mappingGap("poll event id invalid")
            if (events.values.isEmpty()) {
                return@repeat
            }
            when (val update = agentGroupDataUpdate(events)) {
                EventQueueGetResult.TimedOut -> return@repeat
                else -> return update
            }
        }
        return EventQueueGetResult.TimedOut
    }

    private fun agentGroupDataUpdate(events: LlsdValue.ArrayValue): EventQueueGetResult {
        for (event in events.values) {
            val eventFields = (event as? LlsdValue.MapValue)?.values ?: return mappingGap("event entry invalid")
            val message = eventFields["message"]?.asString() ?: return mappingGap("event message absent")
            if (message != AGENT_GROUP_DATA_UPDATE) {
                continue
            }
            val body = (eventFields["body"] as? LlsdValue.MapValue)?.values
                ?: return mappingGap("agent group event body invalid")
            return parseAgentGroupDataUpdate(body)
        }
        return EventQueueGetResult.TimedOut
    }

    private fun parseAgentGroupDataUpdate(body: Map<String, LlsdValue>): EventQueueGetResult {
        if (!hasAgentData(body["AgentData"])) {
            return mappingGap("agent group event agent data invalid")
        }
        val groups = (body["GroupData"] as? LlsdValue.ArrayValue)?.values
            ?: return mappingGap("agent group event group data invalid")
        val newGroupData = body["NewGroupData"]
        val newGroups = when (newGroupData) {
            null -> null
            is LlsdValue.ArrayValue -> newGroupData.values
            else -> return mappingGap("agent group event profile data invalid")
        }
        if (newGroups != null && newGroups.size != groups.size) {
            return mappingGap("agent group event profile count invalid")
        }
        return EventQueueGetResult.AgentGroupDataUpdate(
            groups = groups.mapIndexed { index, value ->
                val fields = (value as? LlsdValue.MapValue)?.values
                    ?: return mappingGap("agent group entry invalid")
                LibomvGroupSnapshot(
                    groupId = fields["GroupID"]?.asString() ?: return mappingGap("agent group id invalid"),
                    displayName = fields["GroupName"]?.asString() ?: return mappingGap("agent group name invalid"),
                    powers = LibomvOsdLongParser.parse(fields["GroupPowers"]),
                    acceptsNotices = fields["AcceptNotices"]?.asBoolean()
                        ?: return mappingGap("agent group notice preference invalid"),
                ).also {
                    if (newGroups != null) {
                        val newGroupFields = (newGroups[index] as? LlsdValue.MapValue)?.values
                            ?: return mappingGap("agent group profile entry invalid")
                        newGroupFields["ListInProfile"]?.asBoolean()
                            ?: return mappingGap("agent group profile visibility invalid")
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

    private fun execute(request: ProtocolHttpRequest): EventQueueHttpResult = try {
        EventQueueHttpResult.Success(httpClient.execute(request))
    } catch (ex: ProtocolHttpException) {
        EventQueueHttpResult.Failed(
            transportMessage(ex.message ?: "protocol http request failed"),
        )
    }

    private fun transportGap(detail: String): EventQueueGetResult.TransportGap =
        EventQueueGetResult.TransportGap(transportMessage(detail))

    private fun mappingGap(detail: String): EventQueueGetResult.MappingGap =
        EventQueueGetResult.MappingGap(eventMessage(detail))

    private fun transportMessage(detail: String): String =
        "current groups transport unavailable: ${SafeDiagnosticRedaction.redact(detail)}"

    private fun eventMessage(detail: String): String =
        "current groups event invalid: ${SafeDiagnosticRedaction.redact(detail)}"

    private fun responseDiagnostic(response: ProtocolHttpResponse): String =
        SafeDiagnosticRedaction.redact("${response.redactedSummary}; ${bodyDiagnostic(response.body)}")

    private fun bodyDiagnostic(body: ByteArray): String =
        SafeDiagnosticRedaction.excerpt(body.decodeToString())
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"

    private fun request(url: String, body: String, timeout: GemDelay): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = url,
        headers = mapOf("Content-Type" to LLSD_XML),
        body = ProtocolHttpBody.TextBody(body, LLSD_XML),
        timeout = timeout,
    )

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
        const val AGENT_GROUP_DATA_UPDATE = "AgentGroupDataUpdate"
    }
}

internal sealed interface EventQueueGetResult {
    data object TimedOut : EventQueueGetResult
    data class TransportGap(
        val redactedMessage: String = "current groups transport unavailable",
    ) : EventQueueGetResult
    data class MappingGap(
        val redactedMessage: String = "current groups event invalid",
    ) : EventQueueGetResult
    data class AgentGroupDataUpdate(val groups: List<LibomvGroupSnapshot>) : EventQueueGetResult
}

private sealed interface EventQueueHttpResult {
    data class Success(val response: ProtocolHttpResponse) : EventQueueHttpResult
    data class Failed(val redactedMessage: String) : EventQueueHttpResult
}
