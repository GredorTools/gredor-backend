package se.gredor.backend.bolagsverket

import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar

interface BolagsverketService {
    fun getRecords(orgnr: String): RecordsResponse

    fun prepareSubmission(personalNumber: String, foretagOrgnr: String): PreparationResponse

    fun validateSubmission(personalNumber: String, foretagOrgnr: String, ixbrl: ByteArray): KontrolleraSvar

    fun submitSubmission(
        personalNumber: String,
        foretagOrgnr: String,
        ixbrl: ByteArray,
        aviseringEpost: String
    ): InlamningOK
}
