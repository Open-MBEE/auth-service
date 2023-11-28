package org.openmbee.flexo.mms.auth.plugins

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

                val token = generateJWT(audience = jwtAudience, issuer = issuer, secret = secret, principal)
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
