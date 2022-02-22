package org.openmbee.mms5.auth.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import org.openmbee.mms5.auth.UserDetailsPrincipal
import org.openmbee.mms5.auth.ldapAuthenticate
import org.openmbee.mms5.auth.ldapEscape
import java.util.*


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

            get("/groups") {
                val ldapConfigValues = getLdapConfValues(environment.config)
                val principal = call.principal<UserDetailsPrincipal>()!!

                val serviceAccount = UserPasswordCredential(
                    environment.config.property("ldap.service_account.name").getString(),
                    environment.config.property("ldap.service_account.pass").getString()
                )
                val userGroups = ldapAuthenticate(
                    serviceAccount,
                    environment.config.propertyOrNull("ldap.location")?.getString() ?: "",
                    ldapConfigValues.userDnPattern,
                    ldapConfigValues.base,
                    ldapConfigValues.groupAttribute,
                    ldapConfigValues.groupSearch.format(
                        ldapConfigValues.userDnPattern.format(ldapEscape(principal.name))
                    )
                )
                call.respond(hashMapOf("groups" to userGroups?.groups))
            }
        }
    }
}
