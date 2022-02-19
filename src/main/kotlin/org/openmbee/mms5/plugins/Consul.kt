package org.openmbee.mms5.plugins

import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.KeyValueClient
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


fun Application.registerApplication() {
    install(ConsulFeature) {
        consulUrl = environment.config.propertyOrNull("consul.service.url")?.getString() ?: "http://localhost:8500"
        consulToken = environment.config.propertyOrNull("consul.service.token")?.getString() ?: "1234567"
        consulServiceId = environment.config.propertyOrNull("consul.service.id")?.getString() ?: "1"
        consulServiceName = environment.config.propertyOrNull("consul.service.name")?.getString() ?: ""
        consulServicePort = environment.config.propertyOrNull("consul.service.port")?.getString()?.toInt() ?: 0
        consulServiceTags = environment.config.propertyOrNull("consul.service.tags")?.getList() ?: emptyList()
    }

    routing {
        get("/healthcheck") {
            call.respond(hashMapOf("status" to "healthy"))
        }
    }
}

class ConsulFeature() {
    class Config {
        var consulUrl: String = ""
        var consulToken: String = ""
        var consulServiceId: String = ""
        var consulServiceName: String = ""
        var consulServicePort: Int = 0
        var consulServiceTags: List<String> = emptyList()

        fun build(): ConsulFeature = ConsulFeature()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Config, ConsulFeature> {
        override val key = AttributeKey<ConsulFeature>("ConsulFeature")

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit): ConsulFeature {
            val configuration = Config().apply(configure)

            println("Consul Registration starting...")

            val consulClient = Consul.builder()
                .withUrl(configuration.consulUrl)
                .withHostnameVerifier(CustomHostnameVerifier)
                .withTokenAuth(configuration.consulToken)
                .build()

            val agentClient: AgentClient = consulClient.agentClient()
            val service: Registration = ImmutableRegistration.builder()
                .id(configuration.consulServiceId)
                .name(configuration.consulServiceName)
                .port(configuration.consulServicePort)
                .check(Registration.RegCheck.ttl(300L))
                .tags(configuration.consulServiceTags)
                .meta(Collections.singletonMap("version", "1.0"))
                .build()
            agentClient.register(service)
            agentClient.pass(configuration.consulServiceId)
            return configuration.build()
        }
    }
}

open class CustomHostnameVerifier: HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return true
    }
    companion object HostnameVerifier : CustomHostnameVerifier()
}