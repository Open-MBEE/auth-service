package org.openmbee.mms5.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import org.openmbee.mms5.UserDetailsPrincipal
import java.util.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("ldapAuth") {
            get("/login") {
                val principal = call.principal<UserDetailsPrincipal>()!!
                val jwtAudience = environment.config.property("jwt.audience").getString()
                val issuer = environment.config.property("jwt.domain").getString()
                val secret = environment.config.property("jwt.secret").getString()

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(issuer)
                    .withClaim("username", principal.name)
                    .withClaim("groups", principal.groups)
                    .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                    .sign(Algorithm.HMAC256(secret))
                call.respond(hashMapOf("token" to token))
            }
        }
    }
}
