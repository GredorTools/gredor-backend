package se.gredor.backend.rest.v1.resources

import io.quarkus.security.UnauthorizedException
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.CookieParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthConsts.TOKEN_COOKIE_NAME
import se.gredor.backend.auth.AuthService
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.filter.GredorRestResource
import se.gredor.backend.rest.v1.model.auth.AuthStatusRequest
import se.gredor.backend.rest.v1.model.auth.AuthStatusResponse

@Path("/v1/auth/")
@GredorRestResource(PerResourceString.AUTH)
class AuthResource {
    @Inject
    private lateinit var authService: AuthService

    @POST
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    fun status(
        @Valid request: AuthStatusRequest,
        @CookieParam(PERSONAL_NUMBER_COOKIE_NAME) personalNumberFromCookie: String?,
        @CookieParam(TOKEN_COOKIE_NAME) tokenFromCookie: String?,
    ): AuthStatusResponse {
        try {
            if (personalNumberFromCookie != null
                && personalNumberFromCookie == request.personalNumber
                && tokenFromCookie != null
                && authService.verifyToken(personalNumberFromCookie, tokenFromCookie)
            ) {
                return AuthStatusResponse(
                    loggedIn = true,
                )
            }
        } catch (_: UnauthorizedException) {
            // Ej inloggad
        }

        return AuthStatusResponse(
            loggedIn = false,
        )
    }
}
