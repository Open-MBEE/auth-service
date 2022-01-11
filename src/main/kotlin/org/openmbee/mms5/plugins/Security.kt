package org.openmbee.mms5.plugins

import io.ktor.auth.*
import io.ktor.auth.ldap.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*

fun Application.configureSecurity() {
    val ldapServerLocation = environment.config.propertyOrNull("ldap.location")?.getString() ?: ""
    val ldapUserDnPattern = environment.config.propertyOrNull("ldap.pattern")?.getString() ?: ""

    authentication {
    	basic(name = "localAuth") {
    		realm = "MMS5 Basic"
    		validate { credentials ->
    			if (credentials.name == credentials.password) {
    				UserIdPrincipal(credentials.name)
    			} else {
    				null
    			}
    		}
    	}

        basic("ldapAuth") {
            realm = "MMS5 LDAP"
            validate { credential ->
                ldapAuthenticate(
                    credential,
                    "ldaps://${ldapServerLocation}",
                    ldapUserDnPattern
                )
            }
        }

        jwt {
            val jwtAudience = environment.config.property("jwt.audience").getString()
            val issuer = environment.config.property("jwt.domain").getString()
            val secret = environment.config.property("jwt.secret").getString()
            realm = environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(jwtAudience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
