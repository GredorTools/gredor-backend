package se.gredor.backend.rest.v1.model.gredor

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class ValidationRequest(

    @field:Pattern(regexp = "^\\d{10}$")
    val foretagOrgnr: String,

    @Poko.ReadArrayContent
    val ixbrl: ByteArray

)
