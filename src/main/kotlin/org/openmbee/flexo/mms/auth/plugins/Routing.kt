package org.openmbee.flexo.mms.auth.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.openmbee.flexo.mms.auth.UserDetailsPrincipal
import java.util.*

@OptIn(InternalAPI::class)
fun Application.configureRouting() {
    val environment = environment
    install(CallLogging)

    routing {
        authenticate("ldapAuth") {
            get("/login") {
                val principal = call.principal<UserDetailsPrincipal>()!!
                val jwtAudience = environment.config.property("jwt.audience").getString()
                val issuer = environment.config.property("jwt.domain").getString()
                val secret = environment.config.property("jwt.secret").getString()

                val expires = Date(System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000))
                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(issuer)
                    .withClaim("username", principal.name)
                    .withClaim("groups", principal.groups)
                    .withExpiresAt(expires)
                    .sign(Algorithm.HMAC256(secret))
                call.respond(hashMapOf("token" to token))
            }
        }

        authenticate("jwtAuth", optional = true) {
            get("/") {
                call.respondText("Hello World!")
            }
            get("/check") {
                val user = call.principal<UserDetailsPrincipal>()
                call.respond(hashMapOf("user" to user))
            }
        }
    }
}
