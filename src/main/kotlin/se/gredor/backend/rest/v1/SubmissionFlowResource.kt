package se.gredor.backend.rest.v1

import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthenticationRequired
import se.gredor.backend.bolagsverket.BolagsverketService
import se.gredor.backend.bolagsverket.PreparationResponse
import se.gredor.backend.rest.v1.model.gredor.PreparationRequest
import se.gredor.backend.rest.v1.model.gredor.SubmissionRequest
import se.gredor.backend.rest.v1.model.gredor.ValidationRequest
import se.gredor.backend.rest.v1.util.createErrorResponse

@Path("/v1/submission-flow/")
@AuthenticationRequired
class SubmissionFlowResource {
    @Inject
    private lateinit var bolagsverketService: BolagsverketService

    @POST
    @Path("prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun prepare(
        @Valid preparationRequest: PreparationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): PreparationResponse {
        return bolagsverketService.prepareSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "Personal number is required"
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
        @Valid validationRequest: ValidationRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): KontrolleraSvar {
        return bolagsverketService.validateSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "Personal number is required"
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
        @Valid submissionRequest: SubmissionRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumber: String?
    ): InlamningOK {
        return bolagsverketService.submitSubmission(
            personalNumber ?: throw BadRequestException(
                createErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "Personal number is required"
                )
            ),
            submissionRequest.foretagOrgnr,
            submissionRequest.ixbrl,
            submissionRequest.aviseringEpost
        )
    }
}
