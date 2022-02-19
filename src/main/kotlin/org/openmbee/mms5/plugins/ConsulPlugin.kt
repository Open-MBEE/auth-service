package org.openmbee.mms5.plugins

import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


fun Application.registerService() {
    install(ConsulPlugin) {
        consulUrl = environment.config.propertyOrNull("consul.service.url")?.getString() ?: "http://localhost:8500"
        consulToken = environment.config.propertyOrNull("consul.service.token")?.getString() ?: ""
        consulServiceName = environment.config.propertyOrNull("consul.service.name")?.getString() ?: "auth-service"
        consulServicePort = environment.config.propertyOrNull("consul.service.port")?.getString()?.toInt() ?: 8080
        consulServiceTags = environment.config.propertyOrNull("consul.service.tags")?.getList() ?: emptyList()
    }

    routing {
        get("/healthcheck") {
            val consulUrl = environment.config.propertyOrNull("consul.service.url")?.getString() ?: "http://localhost:8500"
            val consulToken = environment.config.propertyOrNull("consul.service.token")?.getString() ?: ""
            val client = ConsulPlugin.getConsulClient(consulUrl, consulToken)
            client.agentClient().pass(ConsulPlugin.getServiceId())
            call.respond(hashMapOf("status" to "healthy"))
        }
    }
}

class ConsulPlugin {
    class Config {
        var consulUrl: String = ""
        var consulToken: String = ""
        var consulServiceName: String = ""
        var consulServicePort: Int = 8080
        var consulServiceTags: List<String> = emptyList()

        fun build(): ConsulPlugin = ConsulPlugin()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Config, ConsulPlugin> {
        private val serviceId = UUID.randomUUID().toString()
        override val key = AttributeKey<ConsulPlugin>("ConsulFeature")

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit): ConsulPlugin {
            val configuration = Config().apply(configure)

            println("Consul Registration starting...")

            val agentClient = getConsulClient(configuration.consulUrl, configuration.consulToken).agentClient()
            val service = ImmutableRegistration.builder()
                .id(serviceId)
                .name(configuration.consulServiceName)
                .port(configuration.consulServicePort)
                .check(Registration.RegCheck.ttl(300L))
                .tags(configuration.consulServiceTags)
                .meta(Collections.singletonMap("version", "1.0"))
                .build()
            agentClient.register(service)
            agentClient.pass(serviceId)
            return configuration.build()
        }

        fun getConsulClient(consulUrl: String, consulToken: String): Consul {
            return Consul.builder()
                .withUrl(consulUrl)
                .withHostnameVerifier(CustomHostnameVerifier)
                .withTokenAuth(consulToken)
                .build()
        }

        fun getServiceId(): String {
            return this.serviceId
        }
    }
}

// TODO: Remove when unnecessary?
open class CustomHostnameVerifier: HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return true
    }
    companion object HostnameVerifier : CustomHostnameVerifier()
}
