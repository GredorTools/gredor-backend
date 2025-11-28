package se.gredor.backend.rest.v1

import GeneralExceptionMapper.Companion.createTechnicalErrorResponse
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.Fel
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthenticationRequired
import se.gredor.backend.bolagsverket.BolagsverketPreparationResponse
import se.gredor.backend.bolagsverket.BolagsverketService
import se.gredor.backend.rest.v1.model.bolagsverket.BolagsverketPreparationRequest
import se.gredor.backend.rest.v1.model.bolagsverket.BolagsverketSubmissionRequest
import se.gredor.backend.rest.v1.model.bolagsverket.BolagsverketValidationRequest
import se.gredor.backend.rest.v1.util.createErrorResponse


@Path("/v1/submission-flow/")
@AuthenticationRequired
class SubmissionFlowResource {
    @Inject
    internal lateinit var logger: Logger

    @Inject
    private lateinit var bolagsverketService: BolagsverketService

    @POST
    @Path("prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun prepare(
        @Valid preparationRequest: BolagsverketPreparationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): BolagsverketPreparationResponse {
        return bolagsverketService.prepareSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    ERROR_TEXT_PERSNR_REQUIRED
                )
            ),
            preparationRequest.foretagOrgnr
        )
    }

    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun validate(
        @Valid validationRequest: BolagsverketValidationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): KontrolleraSvar {
        return bolagsverketService.validateSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    ERROR_TEXT_PERSNR_REQUIRED
                )
            ),
            validationRequest.foretagOrgnr,
            validationRequest.ixbrl
        )
    }

    @POST
    @Path("submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submit(
        @Valid submissionRequest: BolagsverketSubmissionRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): InlamningOK {
        return bolagsverketService.submitSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    ERROR_TEXT_PERSNR_REQUIRED
                )
            ),
            submissionRequest.foretagOrgnr,
            submissionRequest.ixbrl,
            submissionRequest.aviseringEpost
        )
    }

    @ServerExceptionMapper
    fun handleClientWebApplicationException(exception: ClientWebApplicationException): Response {
        if (exception.response.status == 400 && exception.response.hasEntity()) {
            try {
                val fel = exception.response.readEntity(Fel::class.java)
                logger.error("Error from Bolagsverket: $fel")

                return if (fel.kod == 9004) {
                    // Felmeddelande "Tekniskt felaktig request" - detta beror troligtvis på fel i Gredor så vi
                    // returnerar tekniskt fel
                    createTechnicalErrorResponse()
                } else {
                    // Skicka annars vidare felmeddelandet från Bolagsverket
                    Response.status(exception.response.status).type(MediaType.TEXT_PLAIN).entity(fel.text)
                        .build()
                }
            } catch (_: ProcessingException) {
                // Inte ett Fel-objekt
            }
        }

        return createTechnicalErrorResponse()
    }
}

private const val ERROR_TEXT_PERSNR_REQUIRED = "Personal number is required"
