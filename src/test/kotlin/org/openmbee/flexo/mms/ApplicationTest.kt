package org.openmbee.flexo.mms

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWT
import java.util.*
import org.junit.jupiter.api.Test
import org.openmbee.flexo.mms.auth.module
import kotlin.test.assertEquals

class ApplicationTest {
    private val issuer = "https://localhost/"
    private val audience = "test-audience"
    private val secret = "testsecret"
    private val relm = "Test Relm"

    @Test
    fun userCanAuthenticateWithJWT() = testApplication {

        environment {
            config = MapApplicationConfig(
                "jwt.audience" to audience,
                "jwt.realm" to relm,
                "jwt.domain" to issuer,
                "jwt.secret" to secret,
            )
        }

        application {
            module()
        }

        val token = generateJWT(username = "test name", groups = listOf("all"))
        val authTest = client.get("/") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.bodyAsText()

        assertEquals("Hello World!", authTest)
    }

    private fun generateJWT(username: String, groups: List<String>): String {

        val algorithm = Algorithm.HMAC256(secret)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("username", username)
            .withArrayClaim("groups", groups.toTypedArray())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }
}