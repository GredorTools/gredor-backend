package se.gredor.backend.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.rest")
interface RestConfig {

    @WithName("verify-signer")
    fun verifySigner(): Boolean

}