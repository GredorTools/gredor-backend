package se.gredor.backend.rest.v1

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/v1/ping/")
class PingResource {
    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    fun status(): String = "pong"
}
