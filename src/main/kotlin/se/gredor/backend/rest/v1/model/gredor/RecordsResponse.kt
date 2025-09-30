package se.gredor.backend.rest.v1.model.gredor

import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod

data class RecordsResponse(
    val foretagsnamn: String,
    val rakenskapsperioder: List<Rakenskapsperiod>
)
