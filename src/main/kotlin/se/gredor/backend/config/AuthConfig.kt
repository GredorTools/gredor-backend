package se.gredor.backend.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.auth")
interface AuthConfig {

    @WithName("limit.per-interval")
    fun limitPerInterval(): Long

    @WithName("limit.per-interval-and-person")
    fun limitPerIntervalAndPerson(): Long

    @WithName("limit.interval-in-hours")
    fun intervalInHours(): Long

}
