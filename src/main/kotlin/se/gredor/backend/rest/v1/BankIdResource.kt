package se.gredor.backend.rest.v1

import io.quarkus.security.UnauthorizedException
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
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthConsts.TOKEN_COOKIE_NAME
import se.gredor.backend.auth.AuthService
import se.gredor.backend.bankid.AuthStatus
import se.gredor.backend.bankid.BankIdService
import se.gredor.backend.bankid.BankIdStatusResponse
import se.gredor.backend.bankid.StatusCompleteData
import se.gredor.backend.config.RestConfig
import se.gredor.backend.rest.v1.model.bankid.AuthInitRequest
import se.gredor.backend.rest.v1.model.bankid.AuthStatusRequest
import se.gredor.backend.rest.v1.model.bankid.CancelRequest
import se.gredor.backend.rest.v1.util.createErrorResponse
import se.gredor.backend.rest.v1.util.resolveEndUserIp

@Path("/v1/bankid")
class BankIdResource {
    @Inject
    private lateinit var authService: AuthService

    @Inject
    private lateinit var bankIdService: BankIdService

    @Inject
    private lateinit var restConfig: RestConfig

    @POST
    @Path("/init")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authInit(
        @Valid request: AuthInitRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumberFromCookie: String?,
        @CookieParam(TOKEN_COOKIE_NAME) tokenFromCookie: String?,
        @Context context: RoutingContext
    ): BankIdStatusResponse {
        try {
            try {
                // Om man redan är legitimerad ska man bara släppas igenom
                if (personalNumberFromCookie != null
                    && personalNumberFromCookie == request.personalNumber
                    && tokenFromCookie != null
                    && authService.verifyToken(personalNumberFromCookie, tokenFromCookie)
                ) {
                    return BankIdStatusResponse(
                        status = AuthStatus.COMPLETE,
                        statusCompleteData = StatusCompleteData(
                            personalNumber = personalNumberFromCookie,
                            token = tokenFromCookie
                        )
                    )
                }
            } catch (_: UnauthorizedException) {
            }

            // Begränsa antal legitimeringar
            if (!authService.isWithinAuthLimit(request.personalNumber)) {
                throw BadRequestException(
                    createErrorResponse(Status.BAD_REQUEST, "Too many authentications")
                )
            }

            // Skicka förfrågan till BankID
            val response = bankIdService.authInit(request.personalNumber, resolveEndUserIp(context, restConfig))
            updateCookies(response, context)

            return response
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, "Invalid parameters"), e
            )
        }
    }

    @POST
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authStatus(@Valid request: AuthStatusRequest, @Context context: RoutingContext): BankIdStatusResponse {
        try {
            val response = bankIdService.authStatus(request.orderRef)
            updateCookies(response, context)

            return response
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, "Invalid parameters"), e
            )
        }
    }

    @POST
    @Path("/cancel")
    @Consumes(MediaType.APPLICATION_JSON)
    fun cancel(@Valid request: CancelRequest): Response {
        try {
            bankIdService.cancel(request.orderRef)
            return Response.ok().build()
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                createErrorResponse(Status.BAD_REQUEST, "Invalid parameters"), e
            )
        }
    }

    private fun updateCookies(response: BankIdStatusResponse, context: RoutingContext) {
        if (response.status == AuthStatus.COMPLETE && response.statusCompleteData != null) {
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
