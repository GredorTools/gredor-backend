package se.gredor.backend.rest.v1.model.auth

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class AuthStatusRequest(

    @field:Pattern(regexp = "^\\d{12}$")
    val personalNumber: String

)
