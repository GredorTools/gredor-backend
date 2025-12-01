package se.gredor.backend.rest

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.rest")
interface RestConfig {

    @WithName("use-x-real-ip")
    fun useXRealIp(): Boolean

}
