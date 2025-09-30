package se.gredor.backend.rest.v1.model.gredor

import java.time.LocalDate

data class PreparationResponse(
    val avtalstext: String,
    val avtalstextAndrad: LocalDate
)
