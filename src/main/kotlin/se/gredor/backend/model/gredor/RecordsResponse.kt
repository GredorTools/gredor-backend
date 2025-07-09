package se.gredor.backend.model.gredor

import com.fasterxml.jackson.annotation.JsonProperty
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod

data class RecordsResponse(
    @JsonProperty("foretagsnamn")
    val foretagsnamn: String,

    @JsonProperty("rakenskapsperioder")
    val rakenskapsperioder: List<Rakenskapsperiod>,
)
