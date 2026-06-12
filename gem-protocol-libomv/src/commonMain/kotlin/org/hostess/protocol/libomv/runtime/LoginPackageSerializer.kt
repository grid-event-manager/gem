package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.mapping.LoginKeys

internal object LoginPackageSerializer {
    fun toXmlRpc(loginPackage: LoginPackage): String = buildString {
        append("<?xml version=\"1.0\"?>")
        append("<methodCall>")
        append("<methodName>login_to_simulator</methodName>")
        append("<params><param><value><struct>")
        stringField("first", loginPackage.first)
        stringField("last", loginPackage.last)
        stringField(LoginKeys.SECRET, loginPackage.passwd)
        stringField("start", loginPackage.start)
        stringField(LoginKeys.CHANNEL, loginPackage.channel)
        stringField(LoginKeys.VERSION, loginPackage.version)
        stringField(LoginKeys.PLATFORM, loginPackage.platform)
        stringField(LoginKeys.MAC, loginPackage.mac)
        stringField(LoginKeys.ID0, loginPackage.id0)
        stringField(LoginKeys.AGREE_TO_TOS, loginPackage.agreeToTos)
        stringField(LoginKeys.READ_CRITICAL, loginPackage.readCritical)
        integerField("last_exec_event", loginPackage.lastExecEvent)
        stringArrayField("options", loginPackage.options)
        append("</struct></value></param></params>")
        append("</methodCall>")
    }

    private fun StringBuilder.stringField(name: String, value: String) {
        append("<member><name>")
        append(escapeXml(name))
        append("</name><value><string>")
        append(escapeXml(value))
        append("</string></value></member>")
    }

    private fun StringBuilder.integerField(name: String, value: Int) {
        append("<member><name>")
        append(escapeXml(name))
        append("</name><value><i4>")
        append(value)
        append("</i4></value></member>")
    }

    private fun StringBuilder.stringArrayField(name: String, values: List<String>) {
        append("<member><name>")
        append(escapeXml(name))
        append("</name><value><array><data>")
        values.forEach { value ->
            append("<value><string>")
            append(escapeXml(value))
            append("</string></value>")
        }
        append("</data></array></value></member>")
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
