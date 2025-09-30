package se.gredor.backend.rest.v1.model.bankid

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class AuthStatusRequest(

    @field:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    val orderRef: String

)
