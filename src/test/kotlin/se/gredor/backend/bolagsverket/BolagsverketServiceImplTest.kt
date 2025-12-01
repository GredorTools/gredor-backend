package se.gredor.backend.bolagsverket

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.api.InformationApi
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Grunduppgifter
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.InlamningApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.api.KontrollApi
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.*
import java.time.LocalDate
import java.util.*

@QuarkusTest
class BolagsverketServiceImplTest {

    private val mockOrgnr = "5569999999"
    private val mockOrgnamn = "Testbolaget AB"
    private val mockPnr = "198605082380"
    private val mockEmail = "test@example.com"

    @Inject
    lateinit var bolagsverketService: BolagsverketService

    @InjectMock(convertScopes = true)
    @RestClient
    lateinit var informationApi: InformationApi

    @InjectMock(convertScopes = true)
    @RestClient
    lateinit var inlamningApi: InlamningApi

    @InjectMock(convertScopes = true)
    @RestClient
    lateinit var kontrollApi: KontrollApi

    @BeforeEach
    fun setup() {
        assertInstanceOf(BolagsverketServiceImpl::class.java, bolagsverketService)
    }

    @Test
    fun getRecords_returnsDataFromApi() {
        val periods = listOf(
            Rakenskapsperiod().from(LocalDate.of(2023, 1, 1)).tom(LocalDate.of(2023, 12, 31)),
            Rakenskapsperiod().from(LocalDate.of(2024, 1, 1)).tom(LocalDate.of(2024, 12, 31)),
        )

        // Mocka
        every { informationApi.grunduppgifter(any(), mockOrgnr) } returns Grunduppgifter()
            .namn(mockOrgnamn)
            .rakenskapsperioder(periods)

        // Kör och verifiera
        val resp = bolagsverketService.getRecords(mockOrgnr)

        assertEquals(mockOrgnamn, resp.foretagsnamn)
        assertEquals(periods, resp.rakenskapsperioder)
    }

    @Test
    fun prepareSubmission_returnsDataFromApi() {
        val avtalstextAndrad = LocalDate.now()

        // Mocka
        every { inlamningApi.skapaInlamningtoken(any(), any()) } returns SkapaTokenOK()
            .token(UUID.randomUUID())
            .avtalstext("Text")
            .avtalstextAndrad(avtalstextAndrad)

        // Kör och verifiera
        val resp = bolagsverketService.prepareSubmission(mockPnr, mockOrgnr)
        assertEquals("Text", resp.avtalstext)
        assertEquals(avtalstextAndrad, resp.avtalstextAndrad)
    }

    @Test
    fun validateSubmission_returnsDataFromApi() {
        val token = UUID.randomUUID()

        // Mocka
        every { inlamningApi.skapaInlamningtoken(any(), any()) } returns SkapaTokenOK().token(token)

        every { kontrollApi.kontrollera(any(), token, any()) } returns KontrolleraSvar().orgnr(mockOrgnr).utfall(
            listOf(
                KontrolleraUtfall().kod("1337").text("Fel 1"),
                KontrolleraUtfall().kod("1338").text("Fel 2")
            )
        )

        // Kör och verifiera
        val resp = bolagsverketService.validateSubmission(mockPnr, mockOrgnr, getIxbrl())
        assertEquals(mockOrgnr, resp.orgnr)
        assertEquals(2, resp.utfall.size)
        assertEquals("1337", resp.utfall.get(0).kod)
        assertEquals("Fel 1", resp.utfall.get(0).text)
        assertEquals("1338", resp.utfall.get(1).kod)
        assertEquals("Fel 2", resp.utfall.get(1).text)
    }

    @Test
    fun submitSubmission_returnsDataFromApi() {
        val token = UUID.randomUUID()

        // Mocka
        every { inlamningApi.skapaInlamningtoken(any(), any()) } returns SkapaTokenOK().token(token)

        every {
            inlamningApi.inlamning(
                any(),
                token,
                any()
            )
        } returns InlamningOK()
            .orgnr(mockOrgnr)
            .avsandare(mockPnr)
            .undertecknare(mockPnr)
            .url("https://example.com")
            .handlingsinfo(
                Handlingsinfo().idnummer("ABC123").dokumentlangd(123).sha256checksumma(ByteArray(32))
            )

        // Kör och verifiera
        val resp = bolagsverketService.submitSubmission(mockPnr, mockOrgnr, getIxbrl(), mockEmail)
        assertEquals(mockOrgnr, resp.orgnr)
        assertEquals(mockPnr, resp.avsandare)
        assertEquals(mockPnr, resp.undertecknare)
        assertEquals("https://example.com", resp.url)
        assertEquals("ABC123", resp.handlingsinfo?.idnummer)
        assertEquals(123, resp.handlingsinfo?.dokumentlangd)
        assertEquals(32, resp.handlingsinfo?.sha256checksumma?.size)
    }

    fun getIxbrl(): ByteArray =
        Base64.getEncoder().encode(
            this::class.java.getResourceAsStream("/testfil.xhtml")?.bufferedReader()?.readText()
                ?.toByteArray() ?: throw RuntimeException("Could not read test file")
        )
}
