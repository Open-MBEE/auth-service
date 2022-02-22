package org.openmbee.mms5.auth.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.auth.ldap.*
import io.ktor.config.*


fun Application.configureAuthentication() {
    val ldapConfigValues = getLdapConfValues(environment.config)

    authentication {
        basic(name = "localAuth") {
            realm = "MMS5 Basic"
            validate { credentials ->
                if (credentials.name == credentials.password) UserIdPrincipal(credentials.name) else null
            }
        }

        basic("ldapAuth") {
            realm = "MMS5 LDAP"
            validate { credential ->
                ldapAuthenticate(
                    credential,
                    ldapConfigValues.serverLocation,
                    ldapConfigValues.userDnPattern
                )
            }
        }

        jwt {
            val jwtAudience = environment.config.property("jwt.audience").getString()
            val issuer = environment.config.property("jwt.domain").getString()
            val secret = environment.config.property("jwt.secret").getString()
            realm = environment.config.property("jwt.realm").getString()
            verifier(
                    JWT.require(Algorithm.HMAC256(secret))
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

data class LdapConfig(
    val serverLocation: String,
    val base: String,
    val userDnPattern: String,
    val groupAttribute: String,
    val groupSearch: String
)

fun getLdapConfValues(config: ApplicationConfig): LdapConfig {
    val ldapServerLocation = config.propertyOrNull("ldap.location")?.getString() ?: ""
    val ldapBase = config.propertyOrNull("ldap.base")?.getString() ?: ""
    val ldapUserDnPattern = (config.propertyOrNull("ldap.userPattern")?.getString() + "," + ldapBase)
    val ldapGroupAttribute = config.propertyOrNull("ldap.groupAttribute")?.getString() ?: ""
    val ldapGroupSearch = config.propertyOrNull("ldap.groupSearchFilter")?.getString() ?: ""
    return LdapConfig(
        ldapServerLocation,
        ldapBase,
        ldapUserDnPattern,
        ldapGroupAttribute,
        ldapGroupSearch
    )
}
