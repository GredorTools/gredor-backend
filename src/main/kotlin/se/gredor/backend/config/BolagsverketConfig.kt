package se.gredor.backend.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import se.gredor.backend.config.definitions.BolagsverketApiDefinition

@ConfigMapping(prefix = "gredor.bolagsverket")
interface BolagsverketConfig {

    @WithName("hamta-arsredovisningsinformation")
    fun hamtaArsredovisningsinformation(): BolagsverketApiDefinition?

    @WithName("lamna-in-arsredovisning")
    fun lamnaInArsredovisning(): BolagsverketApiDefinition?

}
