package se.gredor.backend.rest.v1.resources

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.filter.GredorRestResource

@Path("/v1/ping/")
@GredorRestResource(PerResourceString.PING)
class PingResource {
    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    fun status(): String = "pong"
}
