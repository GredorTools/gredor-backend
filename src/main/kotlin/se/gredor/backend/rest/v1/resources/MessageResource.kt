package se.gredor.backend.rest.v1.resources

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.rest.v1.filter.GredorRestResource
import se.gredor.backend.rest.v1.model.message.Message
import java.time.Instant

@Path("/v1/message/")
@GredorRestResource(PerResourceString.MESSAGE)
@RunOnVirtualThread
class MessageResource {

    @Inject
    private lateinit var restConfig: RestConfig

    /**
     * Returnerar alla meddelanden som ska visas högst upp i
     * användargränssnittet.
     */
    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    fun messages(): List<Message> {
        return restConfig.messages().orElse(emptyList())
            .filter { !it.startTime().isPresent || it.startTime().get() <= Instant.now() }
            .filter { !it.endTime().isPresent || Instant.now() < it.endTime().get() }
            .map { Message(text = it.text()) }
    }
}
