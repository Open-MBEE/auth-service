package org.openmbee.mms5.plugins

import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import com.orbitz.consul.model.catalog.ServiceWeights
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


fun Application.startConsul() {
    var selfAddressString = ""
    (environment as ApplicationEngineEnvironment).connectors.forEach { connector ->
        selfAddressString = "${connector.host}:${connector.port}"
    }

    install(ConsulFeature) {
        consulUrl = "https://mms5-test.jpl.nasa.gov"
        selfAddress = selfAddressString
    }
}

class ConsulFeature(var consulUrl: String) {
    class Config {
        var consulUrl: String = "https://mms5-test.jpl.nasa.gov"
        var selfAddress: String = "0.0.0.0:8080"

        fun build(): ConsulFeature = ConsulFeature(consulUrl)
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Config, ConsulFeature> {
        override val key = AttributeKey<ConsulFeature>("ConsulFeature")

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit): ConsulFeature {
            val configuration = Config().apply(configure)
            val feature = ConsulFeature(configuration.consulUrl)

            println("Consul Registration starting...")

            pipeline.intercept(ApplicationCallPipeline.Setup) {
                println("pipeline intercepted")

                val consulClient = Consul.builder()
                    .withUrl(feature.consulUrl)
                    .withHostnameVerifier(CustomHostnameVerifier)
                    .withTokenAuth("123456")
                    .build()
                val agentClient: AgentClient = consulClient.agentClient()

                val serviceId = "1"
                val service: Registration = ImmutableRegistration.builder()
                    .id(serviceId)
                    .name("auth-service")
                    .port(8080)
                    .check(Registration.RegCheck.ttl(3L)) // registers with a TTL of 3 seconds
                    .tags(listOf("L1"))
                    .meta(Collections.singletonMap("version", "1.0"))
                    .build()

                agentClient.register(service)
            }
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