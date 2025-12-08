package se.gredor.backend.rest.v1.resources

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.junit.jupiter.api.Test
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.Fel
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar
import se.gredor.backend.auth.AuthService
import se.gredor.backend.bolagsverket.BolagsverketPreparationResponse
import se.gredor.backend.bolagsverket.BolagsverketService
import java.time.LocalDate
import java.util.*

@QuarkusTest
class SubmissionFlowResourceTest {

    val mockPnr = "198605082380"

    val mockOrgnr = "5569999999"

    @InjectMock
    lateinit var authService: AuthService

    @InjectMock
    lateinit var bolagsverketService: BolagsverketService

    @Test
    fun prepare_returnsDataFromService() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true
        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } returns
                BolagsverketPreparationResponse(
                    avtalstext = "mockAvtalstext",
                    avtalstextAndrad = LocalDate.of(2024, 1, 1)
                )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(200)
            .body("avtalstext", equalTo("mockAvtalstext"))
    }

    @Test
    fun validate_returnsDataFromService() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true
        every {
            bolagsverketService.validateSubmission(
                mockPnr,
                mockOrgnr,
                getIxbrl()
            )
        } returns KontrolleraSvar().orgnr(mockOrgnr)

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr","ixbrl":"${getIxbrlAsBase64()}"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/validate")
            .then()
            .statusCode(200)
            .body("orgnr", equalTo(mockOrgnr))
    }

    @Test
    fun submit_returnsDataFromService() {
        val token = UUID.randomUUID().toString()
        val aviseringEpost = "example@example.com"

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true
        every {
            bolagsverketService.submitSubmission(
                mockPnr,
                mockOrgnr,
                getIxbrl(),
                aviseringEpost
            )
        } returns InlamningOK().orgnr(mockOrgnr).avsandare(mockPnr)

        // Kör och verifiera
        val requestJson =
            """{"foretagOrgnr":"$mockOrgnr","ixbrl":"${getIxbrlAsBase64()}","aviseringEpost":"$aviseringEpost"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/submit")
            .then()
            .statusCode(200)
            .body("orgnr", equalTo(mockOrgnr))
            .body("avsandare", equalTo(mockPnr))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_on9004Fel() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true

        val fel = Fel().kod(9004).text("Riktigt obra")
        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(fel).build()
        )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_forwardsFelText_onOtherFelCodes() {
        val token = UUID.randomUUID().toString()
        every { authService.verifyToken(mockPnr, token) } returns true

        val felText = "Exempelfel, något gick obra!"
        val fel = Fel().kod(1234).text(felText)
        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(fel).build()
        )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(400)
            .contentType(MediaType.TEXT_PLAIN)
            .body(equalTo(felText))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_onNonFelEntity() {
        val token = UUID.randomUUID().toString()
        every { authService.verifyToken(mockPnr, token) } returns true

        // Mocka
        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(mapOf("foo" to "bar")).build()
        )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_on400WithoutEntity() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true

        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).build()
        )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_onNon400Status() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true

        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } throws ClientWebApplicationException(
            Response.status(401).build()
        )

        // Kör och verifiera
        val requestJson = """{"foretagOrgnr":"$mockOrgnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .body(requestJson)
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    fun getIxbrl(): ByteArray =
        this::class.java.getResourceAsStream("/testfil.xhtml")?.bufferedReader()?.readText()
            ?.toByteArray() ?: throw RuntimeException("Could not read test file")

    fun getIxbrlAsBase64(): String = Base64.getEncoder().encodeToString(getIxbrl())
}
