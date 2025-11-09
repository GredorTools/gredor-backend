package se.gredor.backend.rest.v1.model.bolagsverket

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class BolagsverketValidationRequest(

    @field:Pattern(regexp = "^\\d{10}$")
    val foretagOrgnr: String,

    @Poko.ReadArrayContent
    val ixbrl: ByteArray

)
