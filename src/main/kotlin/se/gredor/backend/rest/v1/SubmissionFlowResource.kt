package se.gredor.backend.rest.v1

import AuthenticationRequired
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.InlamningApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.KontrollApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.config.BolagsverketConfig
import se.gredor.backend.rest.v1.model.gredor.PreparationRequest
import se.gredor.backend.rest.v1.model.gredor.PreparationResponse
import se.gredor.backend.rest.v1.model.gredor.SubmissionRequest
import se.gredor.backend.rest.v1.model.gredor.ValidationRequest
import java.util.*

@Path("/v1/submission-flow/")
@AuthenticationRequired
class SubmissionFlowResource {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var bolagsverketConfig: BolagsverketConfig

    @Inject
    @RestClient
    private lateinit var inlamningApi: InlamningApi

    @Inject
    @RestClient
    private lateinit var kontrollApi: KontrollApi

    @POST
    @Path("prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun prepare(
        @Valid preparationRequest: PreparationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): PreparationResponse {
        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(personalNumber)
                .orgnr(preparationRequest.foretagOrgnr)
        )
        return PreparationResponse(
            avtalstext = skapaTokenResult.avtalstext,
            avtalstextAndrad = skapaTokenResult.avtalstextAndrad
        )
    }

    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun validate(
        @Valid validationRequest: ValidationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): KontrolleraSvar {
        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(personalNumber)
                .orgnr(validationRequest.foretagOrgnr)
        )

        val handling = Handling()
            .fil(validationRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        val kontrolleraResult = kontrollApi.kontrollera(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            KontrolleraAnrop()
                .handling(handling)
        )

        return kontrolleraResult
    }

    @POST
    @Path("submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submit(
        @Valid submissionRequest: SubmissionRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): InlamningOK {
        val skapaTokenResult = inlamningApi.skapaInlamningtoken(
            getBolagsverketApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(personalNumber)
                .orgnr(submissionRequest.foretagOrgnr)
        )

        val handling = Handling()
            .fil(submissionRequest.ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)

        val inlamningResult = inlamningApi.inlamning(
            getBolagsverketApiUrl(),
            skapaTokenResult.token,
            InlamningAnrop()
                .undertecknare(personalNumber)
                .handling(handling)
                .addEpostadresserItem(submissionRequest.aviseringEpost)
                .addKvittensepostadresserItem(submissionRequest.aviseringEpost)
        )

        try {
            val checksum = Base64.getEncoder().encode(
                inlamningResult?.handlingsinfo?.sha256checksumma
            )
            logger.info("Inlamning OK, checksum: $checksum")
        } catch (e: Exception) {
            logger.warn("Inlamning OK, but could not retrieve checksum.")
        }

        return inlamningResult
    }

    private fun getBolagsverketApiUrl(): String {
        val lamnaInArsredovisningApi = bolagsverketConfig.lamnaInArsredovisning()
            ?: throw IllegalStateException("API Lämna in årsredovisning not configured.")

        return "${lamnaInArsredovisningApi.baseurl()}/${lamnaInArsredovisningApi.version()}"
    }
}
