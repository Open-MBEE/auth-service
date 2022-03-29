package org.openmbee.mms5.auth

import io.ktor.application.*
import org.openmbee.mms5.auth.plugins.configureAuthentication
import org.openmbee.mms5.auth.plugins.configureContentNegotiation
import org.openmbee.mms5.auth.plugins.configureRouting


fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureContentNegotiation()
    configureAuthentication()
    configureRouting()
}
