package se.gredor.backend.testutils

import io.mockk.every
import io.quarkus.test.Mock
import io.smallrye.config.SmallRyeConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Produces
import org.eclipse.microprofile.config.Config
import se.gredor.backend.rest.v1.config.RestConfig
import java.util.*

class RestConfigMockProducer {
    @Inject
    lateinit var config: Config

    @Produces
    @ApplicationScoped
    @Mock
    fun restConfig(): RestConfig {
        return config.unwrap(SmallRyeConfig::class.java)
            .getConfigMapping(RestConfig::class.java)
    }

    companion object {
        fun createDefaultMocks(restConfig: RestConfig) {
            val perResourceStrings: RestConfig.PerResourceStrings = object : RestConfig.PerResourceStrings {
                override fun auth() = Optional.empty<String>()
                override fun bankId() = Optional.empty<String>()
                override fun information() = Optional.empty<String>()
                override fun message() = Optional.empty<String>()
                override fun ping() = Optional.empty<String>()
                override fun submissionFlow() = Optional.empty<String>()
            }
            every { restConfig.closedResources() } returns Optional.of(perResourceStrings)
        }
    }
}
