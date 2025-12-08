package se.gredor.backend.rest.v1.filter

import jakarta.ws.rs.NameBinding
import se.gredor.backend.rest.v1.config.PerResourceString

@NameBinding
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GredorRestResource(
    val value: PerResourceString
)
