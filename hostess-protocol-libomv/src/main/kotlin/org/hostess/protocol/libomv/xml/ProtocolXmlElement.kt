package org.hostess.protocol.libomv.xml

internal data class ProtocolXmlElement(
    val name: String,
    val text: String,
    val children: List<ProtocolXmlElement>,
)
