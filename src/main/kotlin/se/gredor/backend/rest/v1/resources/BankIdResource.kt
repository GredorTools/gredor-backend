package se.gredor.backend.rest.v1.resources

import io.vertx.core.http.Cookie.cookie
import io.vertx.core.http.CookieSameSite
import io.vertx.ext.web.RoutingContext
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status
import org.jboss.logging.Logger
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthConsts.TOKEN_COOKIE_NAME
import se.gredor.backend.auth.AuthService
import se.gredor.backend.bankid.BankIdService
import se.gredor.backend.bankid.BankIdStatus
import se.gredor.backend.bankid.BankIdStatusResponse
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.rest.v1.filter.GredorRestResource
import se.gredor.backend.rest.v1.model.bankid.BankIdCancelRequest
import se.gredor.backend.rest.v1.model.bankid.BankIdInitRequest
import se.gredor.backend.rest.v1.model.bankid.BankIdStatusRequest
import se.gredor.backend.rest.v1.util.createErrorResponse
import se.gredor.backend.rest.v1.util.resolveEndUserIp

@Path("/v1/bankid/")
@GredorRestResource(PerResourceString.BANK_ID)
class BankIdResource {

    private val ERROR_TEXT_INVALID_PARAMETERS = "Ogiltiga parametrar."

    @Inject
    internal lateinit var logger: Logger

    @Inject
    private lateinit var authService: AuthService

    @Inject
    private lateinit var bankIdService: BankIdService

    @Inject
    private lateinit var restConfig: RestConfig

    @POST
    @Path("init")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authInit(@Valid request: BankIdInitRequest, @Context context: RoutingContext): BankIdStatusResponse {
        try {
            // Begränsa antal legitimeringar
            if (!authService.isWithinAuthLimit(request.personalNumber)) {
                logger.warn("BankID authentication prevented due to too many authentications")
                throw WebApplicationException(
                    createErrorResponse(Status.TOO_MANY_REQUESTS, "För många legitimeringar. Försök igen senare.")
                )
            }

            // Skicka förfrågan till BankID
            val response = bankIdService.authInit(request.personalNumber, resolveEndUserIp(context, restConfig))
            updateCookies(response, context)

            return response
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, ERROR_TEXT_INVALID_PARAMETERS), e
            )
        }
    }

    @POST
    @Path("status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authStatus(@Valid request: BankIdStatusRequest, @Context context: RoutingContext): BankIdStatusResponse {
        try {
            val response = bankIdService.authStatus(request.orderRef)
            updateCookies(response, context)

            return response
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, ERROR_TEXT_INVALID_PARAMETERS), e
            )
        }
    }

    @POST
    @Path("cancel")
    @Consumes(MediaType.APPLICATION_JSON)
    fun cancel(@Valid request: BankIdCancelRequest): Response {
        try {
            bankIdService.cancel(request.orderRef)
            return Response.ok().build()
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, ERROR_TEXT_INVALID_PARAMETERS), e
            )
        }
    }

    private fun updateCookies(response: BankIdStatusResponse, context: RoutingContext) {
        if (response.status == BankIdStatus.COMPLETE && response.statusCompleteData != null) {
            // Inloggad!
            context.response().addCookie(
                cookie(PERSONAL_NUMBER_COOKIE_NAME, response.statusCompleteData.personalNumber)
                    .setPath("/")
                    .setHttpOnly(true)
                    .setSecure(true)
                    .setSameSite(CookieSameSite.STRICT)
                    .setMaxAge(604800)
            )
            context.response().addCookie(
                cookie(TOKEN_COOKIE_NAME, response.statusCompleteData.token)
                    .setPath("/")
                    .setHttpOnly(true)
                    .setSecure(true)
                    .setSameSite(CookieSameSite.STRICT)
                    .setMaxAge(604800)
            )
        }
    }
}
