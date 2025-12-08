package se.gredor.backend.rest.v1.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import java.util.*

@ConfigMapping(prefix = "gredor.rest.v1")
interface RestConfig {

    @WithName("use-x-real-ip")
    fun useXRealIp(): Boolean

    @WithName("messages")
    fun messages(): Optional<List<Message>>

    @WithName("closed-resources")
    fun closedResources(): Optional<PerResourceStrings>

    interface PerResourceStrings {
        @WithName("auth")
        fun auth(): Optional<String>

        @WithName("bankid")
        fun bankId(): Optional<String>

        @WithName("information")
        fun information(): Optional<String>

        @WithName("message")
        fun message(): Optional<String>

        @WithName("ping")
        fun ping(): Optional<String>

        @WithName("submission-flow")
        fun submissionFlow(): Optional<String>
    }

    fun interface Message {
        @WithName("text")
        fun text(): String
    }

}
