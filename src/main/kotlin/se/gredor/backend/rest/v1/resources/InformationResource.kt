package se.gredor.backend.rest.v1.resources

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.inject.Inject
import jakarta.validation.constraints.Pattern
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.bolagsverket.BolagsverketRecordsResponse
import se.gredor.backend.bolagsverket.BolagsverketService
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.filter.GredorRestResource

@Path("/v1/information/")
@GredorRestResource(PerResourceString.INFORMATION)
@RunOnVirtualThread
class InformationResource {
    @Inject
    private lateinit var bolagsverketService: BolagsverketService

    /**
     * Returnerar företagsinformation för det givna organisationsnumret,
     * inklusive företagsnamn, senaste räkenskapsår samt huruvida bolaget har
     * VD/likvidator.
     */
    @GET
    @Path("records/{orgnr}")
    @Produces(MediaType.APPLICATION_JSON)
    fun records(@PathParam("orgnr") @Pattern(regexp = "^\\d{10}$") orgnr: String): BolagsverketRecordsResponse {
        return bolagsverketService.getRecords(orgnr)
    }
}
