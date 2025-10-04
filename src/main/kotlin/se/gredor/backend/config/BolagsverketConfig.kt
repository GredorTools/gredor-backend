package se.gredor.backend.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.bolagsverket")
interface BolagsverketConfig {

    @WithName("hamta-arsredovisningsinformation")
    fun hamtaArsredovisningsinformation(): ApiDefinition?

    @WithName("lamna-in-arsredovisning")
    fun lamnaInArsredovisning(): ApiDefinition?

    interface ApiDefinition {
        @WithName("baseurl")
        fun baseurl(): String

        @WithName("version")
        fun version(): String
    }

}
