package org.openmbee.flexo.mms

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.security.KeyPairGenerator
import java.util.*
import org.junit.jupiter.api.Test
import org.openmbee.flexo.mms.auth.module
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun userIsGreetedProperly() = testApplication {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.genKeyPair()


        environment {
            config = MapApplicationConfig(
                "jwt.privateKey" to Base64.getEncoder().encodeToString(keyPair.private.encoded),
                "jwt.issuer" to "issuer.test",
                "jwt.audience" to "audience",
                "jwt.realm" to "realm",
                "jwt.domain" to "domain",
                "jwt.secret" to "123",
            )
        }

        application {
            module()
        }

        val greetings = client.get("/").bodyAsText()
        assertEquals("Hello World!", greetings)
    }
}