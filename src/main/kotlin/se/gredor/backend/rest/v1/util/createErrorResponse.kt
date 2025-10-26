package se.gredor.backend.rest.v1.util

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * Skapar ett REST-svar med felmeddelande.
 */
fun createErrorResponse(status: Response.Status, message: String): Response? = Response.status(status)
    .entity(mapOf("error" to message))
    .type(MediaType.APPLICATION_JSON)
    .build()
