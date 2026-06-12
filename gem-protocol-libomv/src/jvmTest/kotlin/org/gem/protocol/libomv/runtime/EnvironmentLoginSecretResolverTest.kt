package org.gem.protocol.libomv.runtime

import org.gem.core.ports.CredentialHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvironmentLoginSecretResolverTest {
    @Test
    fun `resolves strict env json without transforming protocol secret`() {
        val secret = resolver(
            "HOSTESS_SL_SECRET" to """
                {
                  "loginUri": "${secureUrl("login.example", "/cgi-bin/login.cgi")}",
                  "firstName": "Venue",
                  "lastName": "Host",
                  "sharedSecret": "${"$"}1${"$"}source-ready",
                  "startLocation": "home"
                }
            """.trimIndent(),
        ).resolve(CredentialHandle("HOSTESS_SL_SECRET"))

        requireNotNull(secret)
        assertEquals(secureUrl("login.example", "/cgi-bin/login.cgi"), secret.loginUri)
        assertEquals("Venue", secret.firstName)
        assertEquals("Host", secret.lastName)
        assertEquals("${"$"}1${"$"}source-ready", secret.sharedSecret)
        assertEquals("home", secret.startLocation)
    }

    @Test
    fun `defaults start location to last`() {
        val secret = resolver(
            "HOSTESS_SL_SECRET" to """
                {
                  "loginUri": "${secureUrl("login.example", "/cgi-bin/login.cgi")}",
                  "firstName": "Venue",
                  "lastName": "Host",
                  "sharedSecret": "${"$"}1${"$"}source-ready"
                }
            """.trimIndent(),
        ).resolve(CredentialHandle("HOSTESS_SL_SECRET"))

        assertEquals("last", secret?.startLocation)
    }

    @Test
    fun `supports json string escapes`() {
        val secret = resolver(
            "HOSTESS_SL_SECRET" to """
                {
                  "loginUri": "${secureUrl("login.example", "/cgi-bin/login.cgi")}",
                  "firstName": "Ven\u0075e",
                  "lastName": "Ho\"st",
                  "sharedSecret": "${"$"}1${"$"}source-ready"
                }
            """.trimIndent(),
        ).resolve(CredentialHandle("HOSTESS_SL_SECRET"))

        assertEquals("Venue", secret?.firstName)
        assertEquals("Ho\"st", secret?.lastName)
    }

    @Test
    fun `returns null for unavailable or unusable env names`() {
        val resolver = resolver("HOSTESS_SL_SECRET" to validJson())

        assertNull(resolver.resolve(CredentialHandle("MISSING_ENV")))
        assertNull(resolver.resolve(CredentialHandle("credential-file:/tmp/blocked-credential-source.txt")))
        assertNull(resolver.resolve(CredentialHandle("/tmp/blocked-credential-source.txt")))
        assertNull(resolver.resolve(CredentialHandle("""{"loginUri":"inline"}""")))
    }

    @Test
    fun `returns null for malformed json and non-string values`() {
        assertNull(resolver("HOSTESS_SL_SECRET" to "").resolve(CredentialHandle("HOSTESS_SL_SECRET")))
        assertNull(resolver("HOSTESS_SL_SECRET" to "[${validJson()}]").resolve(CredentialHandle("HOSTESS_SL_SECRET")))
        assertNull(resolver("HOSTESS_SL_SECRET" to """{"loginUri":true}""").resolve(CredentialHandle("HOSTESS_SL_SECRET")))
        assertNull(resolver("HOSTESS_SL_SECRET" to """{"loginUri":"x","loginUri":"y"}""").resolve(CredentialHandle("HOSTESS_SL_SECRET")))
    }

    @Test
    fun `returns null for missing blank or unsupported keys`() {
        assertNull(
            resolver("HOSTESS_SL_SECRET" to validJson(missing = "loginUri"))
                .resolve(CredentialHandle("HOSTESS_SL_SECRET")),
        )
        assertNull(
            resolver("HOSTESS_SL_SECRET" to validJson(sharedSecret = " "))
                .resolve(CredentialHandle("HOSTESS_SL_SECRET")),
        )
        assertNull(
            resolver("HOSTESS_SL_SECRET" to """${validJson().dropLast(1)},"extra":"value"}""")
                .resolve(CredentialHandle("HOSTESS_SL_SECRET")),
        )
        assertNull(
            resolver("HOSTESS_SL_SECRET" to """${validJson().dropLast(1)},"extra":true}""")
                .resolve(CredentialHandle("HOSTESS_SL_SECRET")),
        )
    }

    private fun resolver(vararg values: Pair<String, String>): EnvironmentLoginSecretResolver {
        val env = values.toMap()
        return EnvironmentLoginSecretResolver(env::get)
    }

    private fun validJson(
        loginUri: String = secureUrl("login.example", "/cgi-bin/login.cgi"),
        firstName: String = "Venue",
        lastName: String = "Host",
        sharedSecret: String = "${"$"}1${"$"}source-ready",
        missing: String? = null,
    ): String {
        val fields = linkedMapOf(
            "loginUri" to loginUri,
            "firstName" to firstName,
            "lastName" to lastName,
            "sharedSecret" to sharedSecret,
        )
        if (missing != null) {
            fields.remove(missing)
        }
        return fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":\"$value\""
        }
    }

    private companion object {
        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
