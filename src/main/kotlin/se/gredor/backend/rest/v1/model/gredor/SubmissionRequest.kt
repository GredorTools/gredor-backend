package se.gredor.backend.rest.v1.model.gredor

import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

@Poko
class SubmissionRequest(

    @field:Pattern(regexp = "^\\d{10}$")
    val foretagOrgnr: String,

    @Poko.ReadArrayContent
    val ixbrl: ByteArray,

    @field:Email
    val aviseringEpost: String

)
