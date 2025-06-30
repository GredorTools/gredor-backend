package se.gredor.backend.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.bolagsverket-api")
interface BolagsverketApiConfig {

    @WithName("hamta-arsredovisningsinformation.baseurl")
    fun hamtaArsredovisningsinformationBaseurl(): String?

    @WithName("hamta-arsredovisningsinformation.version")
    fun hamtaArsredovisningsinformationVersion(): String?

    @WithName("lamna-in-arsredovisning.baseurl")
    fun lamnaInArsredovisningBaseurl(): String?

    @WithName("lamna-in-arsredovisning.version")
    fun lamnaInArsredovisningVersion(): String?

}