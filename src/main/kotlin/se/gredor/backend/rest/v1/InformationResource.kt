package se.gredor.backend.rest.v1

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.bolagsverket.BolagsverketService
import se.gredor.backend.bolagsverket.RecordsResponse

@Path("/v1/information/")
class InformationResource {
    @Inject
    private lateinit var bolagsverketService: BolagsverketService

    @GET
    @Path("records/{orgnr}")
    @Produces(MediaType.APPLICATION_JSON)
    fun records(@PathParam("orgnr") orgnr: String): RecordsResponse {
        return bolagsverketService.getRecords(orgnr)
    }
}
