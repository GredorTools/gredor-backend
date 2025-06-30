package se.gredor.backend.model.tellustalk

import com.fasterxml.jackson.annotation.JsonProperty

data class SignedFile(

    @JsonProperty("filename")
    val filename: String? = null,

    @JsonProperty("size")
    val size: Int? = null,

    @JsonProperty("sha256")
    val sha256: String? = null,

    @JsonProperty("content_type")
    val contentType: String? = null

)