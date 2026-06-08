package org.hostess.protocol.libomv.mapping

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class LibomvLoginMappingTest {
    @Test
    fun `classifies successful login response`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(successBody()),
        ).value

        assertEquals("live-session", success.sessionId.value)
        assertEquals(true, success.appearanceState.agentAppearanceService)
        assertEquals(45, success.appearanceState.cofVersion)
        assertEquals("inventory-root-id", success.inventoryRoots.inventoryRootId)
        assertEquals("library-root-id", success.inventoryRoots.libraryRootId)
        assertEquals("library-owner-id", success.inventoryRoots.libraryOwnerId)
        assertEquals(
            LoginInventoryFolder(
                folderId = "landmarks-folder-id",
                parentId = "inventory-root-id",
                ownerId = "agent-id",
                name = "Landmarks",
                typeDefault = 3,
                version = 42,
            ),
            success.inventoryRoots.inventorySkeleton.single(),
        )
        assertEquals(
            LoginInventoryFolder(
                folderId = "library-folder-id",
                parentId = "library-root-id",
                ownerId = "library-owner-id",
                name = "Library Landmarks",
                typeDefault = 3,
                version = 7,
            ),
            success.inventoryRoots.librarySkeleton.single(),
        )
    }

    @Test
    fun `classifies xml rpc successful login response`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(xmlRpcSuccessBody()),
        ).value

        assertEquals("live-session", success.sessionId.value)
        assertEquals("agent-id", success.agentId)
        assertEquals(true, success.appearanceState.agentAppearanceService)
        assertEquals(46, success.appearanceState.cofVersion)
        assertEquals("https://caps.example/seed", success.seedCapability)
        assertEquals("203.0.113.8", success.simulatorIp)
        assertEquals(13000, success.simulatorPort)
        assertEquals((1024L shl 32) or 2048L, success.regionHandle)
        assertEquals(123456789L, success.circuitCode)
        assertEquals("xml-inventory-root-id", success.inventoryRoots.inventoryRootId)
        assertEquals("xml-library-root-id", success.inventoryRoots.libraryRootId)
        assertEquals("xml-library-owner-id", success.inventoryRoots.libraryOwnerId)
        assertEquals(
            LoginInventoryFolder(
                folderId = "xml-landmarks-folder-id",
                parentId = "xml-inventory-root-id",
                ownerId = "agent-id",
                name = "Landmarks",
                typeDefault = 3,
                version = 43,
            ),
            success.inventoryRoots.inventorySkeleton.single(),
        )
        assertEquals(
            LoginInventoryFolder(
                folderId = "xml-library-folder-id",
                parentId = "xml-library-root-id",
                ownerId = "xml-library-owner-id",
                name = "Library Landmarks",
                typeDefault = 3,
                version = 8,
            ),
            success.inventoryRoots.librarySkeleton.single(),
        )
    }

    @Test
    fun `successful login ignores invalid appearance state fields`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    field(LoginKeys.SESSION_ID, "live-session")
                    field(LoginKeys.AGENT_APPEARANCE_SERVICE, "maybe")
                    field(LoginKeys.COF_VERSION, "-1")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertNull(success.appearanceState.agentAppearanceService)
        assertNull(success.appearanceState.cofVersion)
    }

    @Test
    fun `successful login with absent inventory fields keeps empty roots`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    field(LoginKeys.SESSION_ID, "live-session")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals(LoginInventoryRoots.empty(), success.inventoryRoots)
    }

    @Test
    fun `successful login skips malformed skeleton folders`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    field(LoginKeys.AGENT_ID, "agent-id")
                    field(LoginKeys.SESSION_ID, "live-session")
                    append("<key>").append(LoginKeys.INVENTORY_SKELETON).append("</key><array>")
                    append("<map><key>name</key><string>Missing ID</string></map>")
                    folder("usable-folder-id", "parent-folder-id", "Usable", 5, 9)
                    append("</array>")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals(
            LoginInventoryFolder(
                folderId = "usable-folder-id",
                parentId = "parent-folder-id",
                ownerId = "agent-id",
                name = "Usable",
                typeDefault = 5,
                version = 9,
            ),
            success.inventoryRoots.inventorySkeleton.single(),
        )
    }

    @Test
    fun `classifies normal login failure without exposing source text`() {
        val failure = failureFor(LoginKeys.MESSAGE to "Bad password for Venue Host")

        assertEquals(LibomvLoginFailureKind.NORMAL_FAILURE, failure.kind)
        assertEquals("login failed: message=Bad password for Venue Host; login=false", failure.redactedMessage)
    }

    @Test
    fun `classifies terms of service challenge`() {
        val failure = failureFor(LoginKeys.MESSAGE to "Terms of Service acceptance requires agree_to_tos")

        assertEquals(LibomvLoginFailureKind.TOS_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: terms of service required: " +
                "message=Terms of Service acceptance requires agree_to_tos; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies xml rpc terms of service challenge`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                xmlRpcResponse(
                    LoginKeys.LOGIN to xmlRpcString("false"),
                    LoginKeys.MESSAGE to xmlRpcString("Terms of Service acceptance requires agree_to_tos"),
                ),
            ),
        ).value

        assertEquals(LibomvLoginFailureKind.TOS_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: terms of service required: " +
                "message=Terms of Service acceptance requires agree_to_tos; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies critical message challenge`() {
        val failure = failureFor(LoginKeys.REASON to "read_critical must be confirmed")

        assertEquals(LibomvLoginFailureKind.CRITICAL_MESSAGE_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: critical message required: reason=read_critical must be confirmed; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies update challenge`() {
        val failure = failureFor(LoginKeys.ERROR to "mandatory viewer version update")

        assertEquals(LibomvLoginFailureKind.UPDATE_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: viewer update required: error=mandatory viewer version update; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies mfa challenge`() {
        val failure = failureFor(LoginKeys.NEXT_URL to "authentication token required for multi-factor login")

        assertEquals(LibomvLoginFailureKind.MFA_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: mfa token required: login=false; " +
                "next_url=authentication token required for multi-factor login",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies xml rpc fault as login failure`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(xmlRpcFault("Bad password for Venue Host")),
        ).value

        assertEquals(LibomvLoginFailureKind.NORMAL_FAILURE, failure.kind)
        assertEquals("login failed: message=Bad password for Venue Host; login=false", failure.redactedMessage)
    }

    @Test
    fun `redacts sensitive diagnostic material while preserving server message shape`() {
        val failure = failureFor(
            LoginKeys.MESSAGE to "Bad password for " +
                "12345678-1234-1234-1234-123456789abc token=secret https://login.example.invalid/path",
            LoginKeys.NEXT_URL to "https://login.example.invalid/mfa?token=secret",
        )

        assertContains(
            failure.redactedMessage,
            "message=Bad password for [redacted-id] token=[redacted] [redacted-url]",
        )
        assertContains(failure.redactedMessage, "next_url=[redacted-url]")
        assertFalse(failure.redactedMessage.contains("12345678-1234"))
        assertFalse(failure.redactedMessage.contains("login.example.invalid"))
        assertFalse(failure.redactedMessage.contains("token=secret"))
    }

    @Test
    fun `classifies malformed response separately`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse("<llsd><map>".encodeToByteArray()),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
        assertEquals("login response malformed: response=<llsd><map>", failure.redactedMessage)
    }

    @Test
    fun `xml rpc parser fails closed for unsafe document type`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                """
                    <!DOCTYPE methodResponse [
                      <!ENTITY unsafe SYSTEM "file:///etc/passwd">
                    ]>
                    <methodResponse><params><param><value>&unsafe;</value></param></params></methodResponse>
                """.trimIndent().encodeToByteArray(),
            ),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
    }

    @Test
    fun `classifies successful response with missing session as malformed`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
        assertEquals("login response malformed: missing=session_id", failure.redactedMessage)
    }

    @Test
    fun `classifies successful response with non scalar top level fields`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    field(LoginKeys.SESSION_ID, "live-session")
                    append("<key>inventory-skeleton</key><array><string>ignored</string></array>")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals("live-session", success.sessionId.value)
    }

    private fun failureFor(vararg fields: Pair<String, String>): LibomvLoginFailure =
        assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "false")
                    fields.forEach { (key, value) -> field(key, value) }
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

    private fun successBody(): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "true")
        field(LoginKeys.AGENT_ID, "agent-id")
        field(LoginKeys.SESSION_ID, "live-session")
        field(LoginKeys.AGENT_APPEARANCE_SERVICE, "yes")
        field(LoginKeys.COF_VERSION, "45")
        mappedUuid(LoginKeys.INVENTORY_ROOT, LoginKeys.FOLDER_ID, "inventory-root-id")
        mappedUuid(LoginKeys.INVENTORY_LIB_ROOT, LoginKeys.FOLDER_ID, "library-root-id")
        mappedUuid(LoginKeys.INVENTORY_LIB_OWNER, LoginKeys.AGENT_ID, "library-owner-id")
        append("<key>").append(LoginKeys.INVENTORY_SKELETON).append("</key><array>")
        folder("landmarks-folder-id", "inventory-root-id", "Landmarks", 3, 42)
        append("</array>")
        append("<key>").append(LoginKeys.INVENTORY_SKEL_LIB).append("</key><array>")
        folder("library-folder-id", "library-root-id", "Library Landmarks", 3, 7)
        append("</array>")
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun xmlRpcSuccessBody(): ByteArray = xmlRpcResponse(
        LoginKeys.LOGIN to xmlRpcString("true"),
        LoginKeys.SESSION_ID to xmlRpcString("live-session"),
        LoginKeys.AGENT_ID to xmlRpcString("agent-id"),
        LoginKeys.AGENT_APPEARANCE_SERVICE to xmlRpcString("true"),
        LoginKeys.COF_VERSION to xmlRpcInt("46"),
        LoginKeys.SEED_CAPABILITY to xmlRpcString("https://caps.example/seed"),
        LoginKeys.SIM_IP to xmlRpcString("203.0.113.8"),
        LoginKeys.SIM_PORT to xmlRpcInt("13000"),
        LoginKeys.REGION_X to xmlRpcInt("1024"),
        LoginKeys.REGION_Y to xmlRpcInt("2048"),
        LoginKeys.CIRCUIT_CODE to xmlRpcInt("123456789"),
        LoginKeys.INVENTORY_ROOT to xmlRpcMappedUuid(LoginKeys.FOLDER_ID, "xml-inventory-root-id"),
        LoginKeys.INVENTORY_LIB_ROOT to xmlRpcMappedUuid(LoginKeys.FOLDER_ID, "xml-library-root-id"),
        LoginKeys.INVENTORY_LIB_OWNER to xmlRpcMappedUuid(LoginKeys.AGENT_ID, "xml-library-owner-id"),
        LoginKeys.INVENTORY_SKELETON to xmlRpcFolderArray(
            xmlRpcFolder("xml-landmarks-folder-id", "xml-inventory-root-id", "Landmarks", 3, 43),
        ),
        LoginKeys.INVENTORY_SKEL_LIB to xmlRpcFolderArray(
            xmlRpcFolder("xml-library-folder-id", "xml-library-root-id", "Library Landmarks", 3, 8),
        ),
    )

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(key).append("</key><string>").append(value).append("</string>")
    }

    private fun StringBuilder.mappedUuid(key: String, fieldName: String, value: String) {
        append("<key>").append(key).append("</key><array><map>")
        field(fieldName, value)
        append("</map></array>")
    }

    private fun StringBuilder.folder(
        folderId: String,
        parentId: String,
        name: String,
        typeDefault: Int,
        version: Int,
    ) {
        append("<map>")
        field(LoginKeys.FOLDER_ID, folderId)
        field(LoginKeys.PARENT_ID, parentId)
        field(LoginKeys.NAME, name)
        append("<key>").append(LoginKeys.TYPE_DEFAULT).append("</key><integer>").append(typeDefault).append("</integer>")
        append("<key>").append(LoginKeys.VERSION_FIELD).append("</key><integer>").append(version).append("</integer>")
        append("</map>")
    }

    private fun xmlRpcResponse(vararg members: Pair<String, String>): ByteArray = buildString {
        append("<methodResponse><params><param><value><struct>")
        members.forEach { (name, value) -> xmlRpcMember(name, value) }
        append("</struct></value></param></params></methodResponse>")
    }.encodeToByteArray()

    private fun xmlRpcFault(message: String): ByteArray = buildString {
        append("<methodResponse><fault><value><struct>")
        xmlRpcMember("faultCode", xmlRpcInt("1"))
        xmlRpcMember("faultString", xmlRpcString(message))
        append("</struct></value></fault></methodResponse>")
    }.encodeToByteArray()

    private fun StringBuilder.xmlRpcMember(name: String, value: String) {
        append("<member><name>").append(name).append("</name><value>")
        append(value)
        append("</value></member>")
    }

    private fun xmlRpcString(value: String): String = "<string>$value</string>"

    private fun xmlRpcInt(value: String): String = "<i4>$value</i4>"

    private fun xmlRpcMappedUuid(fieldName: String, value: String): String =
        xmlRpcArray(xmlRpcStruct(fieldName to xmlRpcString(value)))

    private fun xmlRpcFolderArray(vararg folders: String): String =
        xmlRpcArray(*folders)

    private fun xmlRpcFolder(
        folderId: String,
        parentId: String,
        name: String,
        typeDefault: Int,
        version: Int,
    ): String = xmlRpcStruct(
        LoginKeys.FOLDER_ID to xmlRpcString(folderId),
        LoginKeys.PARENT_ID to xmlRpcString(parentId),
        LoginKeys.NAME to xmlRpcString(name),
        LoginKeys.TYPE_DEFAULT to xmlRpcInt(typeDefault.toString()),
        LoginKeys.VERSION_FIELD to xmlRpcInt(version.toString()),
    )

    private fun xmlRpcStruct(vararg members: Pair<String, String>): String = buildString {
        append("<struct>")
        members.forEach { (name, value) ->
            append("<member><name>").append(name).append("</name><value>")
            append(value)
            append("</value></member>")
        }
        append("</struct>")
    }

    private fun xmlRpcArray(vararg values: String): String = buildString {
        append("<array><data>")
        values.forEach { value ->
            append("<value>").append(value).append("</value>")
        }
        append("</data></array>")
    }
}
