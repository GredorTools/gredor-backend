package se.gredor.backend.rest.v1.model.bankid

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class BankIdInitRequest(

    @field:Pattern(regexp = "^\\d{12}$")
    val personalNumber: String

)
