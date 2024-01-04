package org.openmbee.flexo.mms

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.*
import org.openmbee.flexo.mms.auth.UserDetailsPrincipal
import org.openmbee.flexo.mms.auth.module
import org.openmbee.flexo.mms.auth.plugins.generateJWT
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile
import java.util.*
import kotlin.test.assertEquals

@Testcontainers
class LDAPAuthenticationTest {
    companion object {
        // JWT Settings
        val issuer = "https://localhost/"
        val audience = "test-audience"
        val secret = "testsecret"
        val relm = "Test Relm"

        // LDAP Settings
        val LDAP_PORT_NUMBER = 1388
        val LDAP_ROOT = "dc=example,dc=org"
        val LDAP_ADMIN_USERNAME = "admin"
        val LDAP_ADMIN_PASSWORD = "adminpassword"
        val LDAP_USER_DC = "users"
        val LDAP_GROUP = "group01"
        val LDAP_PASSWORDS = "password1,password2"
        val LDAP_USERS = "user01,user02"

        //Fuseki
        val FUSEKI_PORT_NUMBER = 3030

        val ldapContainer: GenericContainer<Nothing>  = GenericContainer<Nothing>("bitnami/openldap:2.6.4").apply {
            val ldapENVs: Map<String, String> = mapOf(
                "LDAP_PORT_NUMBER" to "${LDAP_PORT_NUMBER}",
                "LDAP_ROOT" to LDAP_ROOT,
                "LDAP_ADMIN_USERNAME" to LDAP_ADMIN_USERNAME,
                "LDAP_ADMIN_PASSWORD" to LDAP_ADMIN_PASSWORD,
                "LDAP_USERS" to LDAP_USERS,
                "LDAP_USER_DC" to LDAP_USER_DC,
                "LDAP_GROUP" to LDAP_GROUP,
                "LDAP_PASSWORDS" to LDAP_PASSWORDS
            )
            withExposedPorts(LDAP_PORT_NUMBER)
            withEnv(ldapENVs)
            waitingFor(Wait.forLogMessage(".*LDAP setup finished!.*\\n", 1)) // wait for ldap server to start
        }

        val fuseki: GenericContainer<Nothing>  = GenericContainer<Nothing>("atomgraph/fuseki:4.6").apply {
            val fusekiENVs: Map<String, String> = mapOf(
                "JAVA_OPTIONS" to "-Xmx8192m -Xms8192m"
            )
            withCopyFileToContainer(
                MountableFile.forClasspathResource("cluster.trig"),
                "/tmp/mount/"
            )
            withCommand("--file=/tmp/mount/cluster.trig --update /ds")
            withExposedPorts(FUSEKI_PORT_NUMBER)
            withEnv(fusekiENVs)
            waitingFor(Wait.forLogMessage(".*Start Fuseki.*\\n", 1)) // wait for ldap server to start
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            ldapContainer.start()
            fuseki.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            ldapContainer.stop()
            fuseki.stop()
        }
    }

    @Test
    fun testGetLogin() = testApplication {
        environment {
            config = MapApplicationConfig(
                "jwt.audience" to audience,
                "jwt.realm" to relm,
                "jwt.domain" to issuer,
                "jwt.secret" to secret,
                "ldap.location" to "ldap://${ldapContainer.host}:${ldapContainer.getMappedPort(LDAP_PORT_NUMBER)}",
                "ldap.base" to LDAP_ROOT,
                "ldap.groupStore.context" to "http://layer1-service/",
                "ldap.groupStore.uri" to "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT_NUMBER)}/ds/sparql",
                "ldap.groupNamespace" to "ldap/group/",
                "ldap.userNamespace" to "ldap/user/",
                "ldap.groupAttribute" to "cn",
                "ldap.userPattern" to "cn=${LDAP_ADMIN_USERNAME}",
                "ldap.groupSearchFilter" to "(&(objectclass=group)(member=%s)(|(%s)))"
            )
        }
        application {
            module()
        }

        val authString = "${LDAP_ADMIN_USERNAME}:${LDAP_ADMIN_PASSWORD}"
        val authBase64 = Base64.getEncoder().encodeToString(authString.toByteArray())

        client.get("/login"){
            headers {
                append(HttpHeaders.Authorization, "Basic $authBase64")
            }
        }.apply {
            assertEquals("200 OK", this.status.toString())

            val token = Json.parseToJsonElement(this.bodyAsText()).jsonObject["token"]
                .toString()
                .removeSurrounding("\"")

            val claim: Map<String, Claim> = decodeJWT(token, secret)

            // validate JWT
            assertEquals("ldap/user/${LDAP_ADMIN_USERNAME}", claim.get("username")
                .toString()
                .removeSurrounding("\"")
            )
        }
    }

    @Test
    fun testCheckLogin() = testApplication {
        environment {
            config = MapApplicationConfig(
                "jwt.audience" to audience,
                "jwt.realm" to relm,
                "jwt.domain" to issuer,
                "jwt.secret" to secret,
                "ldap.location" to "ldap://${ldapContainer.host}:${ldapContainer.getMappedPort(LDAP_PORT_NUMBER)}",
                "ldap.base" to LDAP_ROOT,
                "ldap.groupStore.context" to "http://layer1-service/",
                "ldap.groupStore.uri" to "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT_NUMBER)}/ds/sparql",
                "ldap.groupNamespace" to "ldap/group/",
                "ldap.userNamespace" to "ldap/user/",
                "ldap.groupAttribute" to "cn",
                "ldap.userPattern" to "cn=${LDAP_ADMIN_USERNAME}",
                "ldap.groupSearchFilter" to "(&(objectclass=group)(member=%s)(|(%s)))"
            )
        }
        application {
            module()
        }
        val name = "test name"
        val groups = listOf("all")
        val principal = UserDetailsPrincipal(name = name, groups = groups)
        val token = generateJWT(issuer = issuer, audience = audience, secret = secret, principal = principal)

        client.get("/check") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals("200 OK", this.status.toString())

            val response = Json.parseToJsonElement(this.bodyAsText()).jsonObject["user"]

            assertEquals(name, (response!!.jsonObject["name"].toString().removeSurrounding("\"")))
            assertEquals(groups.toString(), response.jsonObject["groups"].toString().replace("\"", ""))
        }
    }

    fun decodeJWT(token: String, secret: String): Map<String, Claim> {
        val algorithm = Algorithm.HMAC256(secret)
        val verifier: JWTVerifier = JWT.require(algorithm).build()
        val jwt = verifier.verify(token)

        return jwt.claims
    }
}