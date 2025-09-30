package se.gredor.backend.config.definitions

import io.smallrye.config.WithName

interface BolagsverketApiDefinition {

    @WithName("baseurl")
    fun baseurl(): String

    @WithName("version")
    fun version(): String

}
