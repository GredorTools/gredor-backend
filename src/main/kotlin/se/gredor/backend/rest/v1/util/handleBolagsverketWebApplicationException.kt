package se.gredor.backend.rest.v1.util

import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.Fel
import se.gredor.backend.exceptions.GeneralExceptionMapper.Companion.createTechnicalErrorResponse

fun handleBolagsverketWebApplicationException(exception: ClientWebApplicationException, logger: Logger): Response {
    if (exception.response.status == 400 && exception.response.hasEntity()) {
        try {
            val fel = exception.response.readEntity(Fel::class.java)
            logger.error("Error from Bolagsverket: $fel")

            return if (fel.kod == 9004) {
                // Felmeddelande "Tekniskt felaktig request" - detta beror troligtvis på fel i Gredor så vi
                // returnerar tekniskt fel
                createTechnicalErrorResponse()
            } else if (fel.kod == 4005) {
                // Specialfall för fel 4005 med originaltext "Ingen träff på efterfrågat organisationsnummer."
                Response.status(exception.response.status).type(MediaType.TEXT_PLAIN)
                    .entity("Ingen träff på efterfrågat organisationsnummer. Observera att Gredor endast har stöd för aktiebolag.")
                    .build()
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
