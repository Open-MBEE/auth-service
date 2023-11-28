package org.openmbee.flexo.mms

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.util.*
import org.junit.jupiter.api.Test
import org.openmbee.flexo.mms.auth.UserDetailsPrincipal
import org.openmbee.flexo.mms.auth.plugins.generateJWT
import org.openmbee.flexo.mms.auth.module
import kotlin.test.assertEquals

class ApplicationTest {
    private val issuer = "https://localhost/"
    private val audience = "test-audience"
    private val secret = "testsecret"
    private val realm = "Test Realm"

    @Test
    fun userCanAuthenticateWithJWT() = testApplication {

        environment {
            config = MapApplicationConfig(
                "jwt.audience" to audience,
                "jwt.realm" to realm,
                "jwt.domain" to issuer,
                "jwt.secret" to secret,
            )
        }

        application {
            module()
        }

        val principal = UserDetailsPrincipal(name = "test name", groups = listOf("all"))
        val token = generateJWT(issuer = issuer, audience = audience, secret = secret, principal = principal)
        val authTest = client.get("/") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.bodyAsText()

        assertEquals("Hello World!", authTest)
    }
}