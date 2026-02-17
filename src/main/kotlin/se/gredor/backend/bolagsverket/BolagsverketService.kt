package se.gredor.backend.bolagsverket

import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar

interface BolagsverketService {
    /**
     * Hämtar företagsinformation från Bolagsverket för det givna organisationsnumret.
     */
    fun getRecords(orgnr: String): BolagsverketRecordsResponse

    /**
     * Hämtar Bolagsverkets avtalstext.
     */
    fun prepareSubmission(personalNumber: String, foretagOrgnr: String): BolagsverketPreparationResponse

    /**
     * Validerar en årsredovisning (iXBRL-fil) mot Bolagsverkets
     * valideringsregler.
     */
    fun validateSubmission(personalNumber: String, foretagOrgnr: String, ixbrl: ByteArray): KontrolleraSvar

    /**
     * Lämnar in en årsredovisning (iXBRL-fil) till Bolagsverket. Aviseringar
     * skickas av Bolagsverket till den angivna e-postadressen.
     */
    fun submitSubmission(
        personalNumber: String,
        foretagOrgnr: String,
        ixbrl: ByteArray,
        aviseringEpost: String
    ): InlamningOK
}
