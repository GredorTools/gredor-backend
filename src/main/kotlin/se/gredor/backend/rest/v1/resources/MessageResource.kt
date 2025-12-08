package se.gredor.backend.rest.v1.resources

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.rest.v1.filter.GredorRestResource
import se.gredor.backend.rest.v1.model.message.Message

@Path("/v1/message/")
@GredorRestResource(PerResourceString.MESSAGE)
class MessageResource {

    @Inject
    private lateinit var restConfig: RestConfig

    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    fun messages(): List<Message> {
        return restConfig.messages().orElse(emptyList()).map { Message(text = it.text()) }
    }
}
