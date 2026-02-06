package se.gredor.backend.exceptions

import MdcRequestFilter.Companion.getRequestId
import io.quarkus.logging.Log
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

@Provider
class GeneralExceptionMapper {
    @ServerExceptionMapper(priority = Priorities.USER + 2)
    fun mapException(exception: Throwable): Response {
        Log.error(exception.message, exception)

        return if (exception is WebApplicationException) {
            // WebApplicationException = troligtvis klientens fel, hantera som vanligt
            exception.response
        } else {
            // Annars troligtvis något internt fel i backend
            createTechnicalErrorResponse()
        }
    }

    companion object {
        fun createTechnicalErrorResponse(): Response {
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                .entity("Tekniskt fel, försök igen senare.\n\nSkulle felet fortsatt uppstå, mejla gredor@potatiz.com och uppge följande felsöknings-ID: ${getRequestId()}")
                .build()
        }
    }
}
