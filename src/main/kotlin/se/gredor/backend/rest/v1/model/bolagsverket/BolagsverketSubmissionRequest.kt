package se.gredor.backend.rest.v1.model.bolagsverket

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

@Poko
class BolagsverketSubmissionRequest(

    @field:Pattern(regexp = "^\\d{10}$")
    val foretagOrgnr: String,

    @Poko.ReadArrayContent
    val ixbrl: ByteArray,

    @field:Email
    val aviseringEpost: String

)
