package org.openmbee.mms5.auth.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.apache.jena.vocabulary.SchemaDO.pattern
import org.openmbee.mms5.auth.UserDetailsPrincipal
import java.util.*


@OptIn(InternalAPI::class)
fun Application.configureRouting() {
    install(CallLogging)

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
