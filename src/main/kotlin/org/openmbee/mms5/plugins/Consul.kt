package org.openmbee.mms5.plugins

import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.util.*
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


fun Application.startConsul() {
    var selfHost: String = ""
    var selfPort: Int = 0
    (environment as ApplicationEngineEnvironment).connectors.forEach { connector ->
        selfHost = connector.host
        selfPort = connector.port
    }

    install(ConsulFeature) {
        consulUrl = environment.config.propertyOrNull("consul.service.url")?.getString() ?: "http://localhost:8500"
        consulToken = environment.config.propertyOrNull("consul.service.token")?.getString() ?: "1234567"
        consulServiceId = environment.config.propertyOrNull("consul.service.id")?.getString() ?: "1"
        selfAddressHost = selfHost
        selfAddressPort = selfPort
    }
}

class ConsulFeature() {
    class Config {
        var consulUrl: String = ""
        var consulToken: String = ""
        var consulServiceId: String = ""
        var selfAddressHost: String = ""
        var selfAddressPort: Int = 8080

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
                .name("auth-service")
                .port(configuration.selfAddressPort)
                .check(Registration.RegCheck.ttl(3L)) // registers with a TTL of 3 seconds
                .tags(listOf("L0"))
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