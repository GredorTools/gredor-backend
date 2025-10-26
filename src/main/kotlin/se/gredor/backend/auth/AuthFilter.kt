package se.gredor.backend.auth

import io.quarkus.security.UnauthorizedException
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import se.gredor.backend.auth.AuthConsts.PERSONAL_NUMBER_COOKIE_NAME
import se.gredor.backend.auth.AuthConsts.TOKEN_COOKIE_NAME
import se.gredor.backend.rest.v1.util.createErrorResponse

@Provider
@Priority(Priorities.AUTHENTICATION)
@AuthenticationRequired
class AuthFilter : ContainerRequestFilter {

    @Inject
    private lateinit var authService: AuthService

    override fun filter(requestContext: ContainerRequestContext) {
        // Hämta token och personnummer från cookies
        val cookies = requestContext.cookies
        val personalNumber = cookies[PERSONAL_NUMBER_COOKIE_NAME]?.value
        val token = cookies[TOKEN_COOKIE_NAME]?.value

        if (token == null || personalNumber == null) {
            abortWithUnauthorized(requestContext, "Missing required cookies")
            return
        }

        // Verifiera med AuthService
        val isValid = try {
            authService.verifyToken(personalNumber, token)
        } catch (_: UnauthorizedException) {
            false
        }

        if (!isValid) {
            abortWithUnauthorized(requestContext, "Invalid authentication token")
        }
    }

    private fun abortWithUnauthorized(requestContext: ContainerRequestContext, message: String) {
        requestContext.abortWith(createErrorResponse(Response.Status.UNAUTHORIZED, message))
    }
}
