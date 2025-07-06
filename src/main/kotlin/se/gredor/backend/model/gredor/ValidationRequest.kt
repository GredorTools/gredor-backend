package se.gredor.backend.model.gredor

import com.fasterxml.jackson.annotation.JsonProperty
import dev.drewhamilton.poko.Poko
import jakarta.validation.constraints.Pattern

@Poko
class ValidationRequest(

    @JsonProperty("companyOrgnr")
    @Pattern(regexp = "^\\d{10}$")
    override val companyOrgnr: String,

    @JsonProperty("signerPnr")
    @Pattern(regexp = "^\\d{12}$")
    override val signerPnr: String,

    @JsonProperty("signedPdf")
    @Poko.ReadArrayContent
    override val signedPdf: ByteArray,

    @JsonProperty("ixbrl")
    @Poko.ReadArrayContent
    val ixbrl: ByteArray

) : AuthenticatableRequest