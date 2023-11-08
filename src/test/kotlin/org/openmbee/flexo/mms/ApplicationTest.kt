package org.openmbee.flexo.mms

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import org.openmbee.flexo.mms.auth.plugins.configureRouting

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ configureRouting() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }
}
