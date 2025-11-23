package se.gredor.backend.bolagsverket

import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.xml.bind.DatatypeConverter.printHexBinary
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.api.InformationApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.InlamningApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.KontrollApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.config.BolagsverketConfig

@UnlessBuildProfile("dev")
@ApplicationScoped
class BolagsverketServiceImpl : BolagsverketService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var bolagsverketConfig: BolagsverketConfig

    @Inject
    @RestClient
    private lateinit var informationApi: InformationApi

    @Inject
    @RestClient
    private lateinit var inlamningApi: InlamningApi

    @Inject
    @RestClient
    private lateinit var kontrollApi: KontrollApi

    override fun getRecords(orgnr: String): BolagsverketRecordsResponse {
        val grunduppgifter = informationApi.grunduppgifter(getHamtaArsredovisningsinformationApiUrl(), orgnr)

        return BolagsverketRecordsResponse(
            foretagsnamn = grunduppgifter.namn,
            rakenskapsperioder = grunduppgifter.rakenskapsperioder
        )
    }

    override fun prepareSubmission(personalNumber: String, foretagOrgnr: String): BolagsverketPreparationResponse {
        logger.info("Preparing submission")

        val skapaTokenResult = retrieveToken(personalNumber, foretagOrgnr)

        return BolagsverketPreparationResponse(
            avtalstext = skapaTokenResult.avtalstext,
            avtalstextAndrad = skapaTokenResult.avtalstextAndrad
        )
    }

    override fun validateSubmission(personalNumber: String, foretagOrgnr: String, ixbrl: ByteArray): KontrolleraSvar {
        logger.info("Validating submission")

        val skapaTokenResult = retrieveToken(personalNumber, foretagOrgnr)

        val kontrolleraResult = kontrollApi.kontrollera(
            getLamnaInArsredovisningApiUrl(),
            skapaTokenResult.token,
            KontrolleraAnrop()
                .handling(createHandling(ixbrl))
        )

        return kontrolleraResult
    }

    override fun submitSubmission(
        personalNumber: String,
        foretagOrgnr: String,
        ixbrl: ByteArray,
        aviseringEpost: String
    ): InlamningOK {
        logger.info("Submitting submission")

        val skapaTokenResult = retrieveToken(personalNumber, foretagOrgnr)

        val inlamningResult = inlamningApi.inlamning(
            getLamnaInArsredovisningApiUrl(),
            skapaTokenResult.token,
            InlamningAnrop()
                .undertecknare(personalNumber)
                .handling(createHandling(ixbrl))
                .addEpostadresserItem(aviseringEpost)
                .addKvittensepostadresserItem(aviseringEpost)
        )

        val inlamningId = inlamningResult?.handlingsinfo?.idnummer ?: "UNKNOWN"
        val inlamningLength = inlamningResult?.handlingsinfo?.dokumentlangd ?: "UNKNOWN"
        val inlamningChecksum =
            inlamningResult?.handlingsinfo?.sha256checksumma?.let { printHexBinary(it) } ?: "UNKNOWN"
        logger.info("Inlamning OK, id: $inlamningId, length: $inlamningLength, checksum: $inlamningChecksum")

        return inlamningResult
    }

    private fun retrieveToken(
        personalNumber: String,
        foretagOrgnr: String
    ): SkapaTokenOK {
        logger.info("Retrieving token")

        return inlamningApi.skapaInlamningtoken(
            getLamnaInArsredovisningApiUrl(),
            SkapaInlamningTokenAnrop()
                .pnr(personalNumber)
                .orgnr(foretagOrgnr)
        )
    }

    private fun createHandling(ixbrl: ByteArray): Handling {
        return Handling()
            .fil(ixbrl)
            .typ(Handling.TypEnum.ARSREDOVISNING_KOMPLETT)
    }

    private fun getHamtaArsredovisningsinformationApiUrl(): String {
        val hamtaArsredovisningsinformationApi = bolagsverketConfig.hamtaArsredovisningsinformation()
            ?: throw IllegalStateException("API Hämta årsredovisningsinformation not configured.")

        return "${hamtaArsredovisningsinformationApi.baseurl()}/${hamtaArsredovisningsinformationApi.version()}"
    }

    private fun getLamnaInArsredovisningApiUrl(): String {
        val lamnaInArsredovisningApi = bolagsverketConfig.lamnaInArsredovisning()
            ?: throw IllegalStateException("API Lämna in årsredovisning not configured.")

        return "${lamnaInArsredovisningApi.baseurl()}/${lamnaInArsredovisningApi.version()}"
    }
}
