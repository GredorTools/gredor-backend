package se.gredor.backend.model.tellustalk

import com.fasterxml.jackson.annotation.JsonProperty

data class Signature(

    @JsonProperty("personal_id")
    var personalId: String? = null,

    @JsonProperty("full_name")
    var fullName: String? = null,

    @JsonProperty("signature_data")
    var signatureData: String? = null,

    @JsonProperty("signed_files")
    var signedFiles: MutableList<SignedFile?>? = null,

    @JsonProperty("given_names")
    var givenNames: String? = null,

    @JsonProperty("family_name")
    var familyName: String? = null,

    @JsonProperty("sign_time")
    var signTime: String? = null,

    @JsonProperty("sign_text")
    var signText: String? = null,

    @JsonProperty("id_service")
    var idService: String? = null

)