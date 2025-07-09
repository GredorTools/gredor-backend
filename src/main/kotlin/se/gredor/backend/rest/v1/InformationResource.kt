package se.gredor.backend.rest.v1

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.api.InformationApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.config.BolagsverketApiConfig
import se.gredor.backend.model.gredor.RecordsResponse

@Path("/v1/information/")
class InformationResource {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var bolagsverketApiConfig: BolagsverketApiConfig

    @Inject
    @RestClient
    private lateinit var informationApi: InformationApi

    @GET
    @Path("records/{orgnr}")
    @Produces(MediaType.APPLICATION_JSON)
    fun records(@PathParam("orgnr") orgnr: String): RecordsResponse {
        val grunduppgifter = informationApi.grunduppgifter(
            "${bolagsverketApiConfig.hamtaArsredovisningsinformationBaseurl()}/${bolagsverketApiConfig.hamtaArsredovisningsinformationVersion()}/",
            orgnr
        )
        return RecordsResponse(
            foretagsnamn = grunduppgifter.namn,
            rakenskapsperioder = grunduppgifter.rakenskapsperioder
        )
    }
}
