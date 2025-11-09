package se.gredor.backend.bolagsverket

import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod
import java.time.LocalDate

data class BolagsverketPreparationResponse(
    val avtalstext: String,
    val avtalstextAndrad: LocalDate
)

data class BolagsverketRecordsResponse(
    val foretagsnamn: String,
    val rakenskapsperioder: List<Rakenskapsperiod>
)
