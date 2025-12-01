package se.gredor.backend.bankid

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gredor.bankid")
interface BankIdConfig {

    @WithName("test-mode")
    fun testMode(): Boolean

    @WithName("client-identifier")
    fun clientIdentifier(): String

    @WithName("cert.path")
    fun certPath(): String

    @WithName("cert.password")
    fun certPassword(): String

}
