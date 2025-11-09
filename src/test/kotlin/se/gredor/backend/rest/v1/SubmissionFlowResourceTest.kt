package se.gredor.backend.rest.v1

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.InlamningOK
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.KontrolleraSvar
import se.gredor.backend.auth.AuthConsts
import se.gredor.backend.auth.AuthService
import se.gredor.backend.bolagsverket.BolagsverketPreparationResponse
import se.gredor.backend.bolagsverket.BolagsverketService
import java.util.*

@QuarkusTest
class SubmissionFlowResourceTest {

    val mockPnr = "198605082380"

    val mockOrgnr = "5569999999"

    val mockIxbrl = "PGh0bWw+PC9odG1sPg=="

    @InjectMock
    lateinit var authService: AuthService

    @InjectMock
    lateinit var bolagsverketService: BolagsverketService

    @Test
    fun prepare_returnsDataFromService() {
        val token = UUID.randomUUID().toString()
        every { authService.verifyToken(mockPnr, token) } returns true
        every { bolagsverketService.prepareSubmission(mockPnr, mockOrgnr) } returns
                BolagsverketPreparationResponse(
                    avtalstext = "mockAvtalstext",
                    avtalstextAndrad = java.time.LocalDate.of(2024, 1, 1)
                )

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body("""{"foretagOrgnr":"$mockOrgnr"}""")
            .post("/v1/submission-flow/prepare")
            .then()
            .statusCode(200)
            .body("avtalstext", equalTo("mockAvtalstext"))
    }

    @Test
    fun validate_returnsDataFromService() {
        val token = UUID.randomUUID().toString()
        every { authService.verifyToken(mockPnr, token) } returns true
        every {
            bolagsverketService.validateSubmission(
                mockPnr,
                mockOrgnr,
                Base64.getDecoder().decode(mockIxbrl)
            )
        } returns KontrolleraSvar().orgnr(mockOrgnr)

        val payload = """{"foretagOrgnr":"$mockOrgnr","ixbrl":"$mockIxbrl"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(payload)
            .post("/v1/submission-flow/validate")
            .then()
            .statusCode(200)
            .body("orgnr", equalTo(mockOrgnr))
    }

    @Test
    fun submit_returnsDataFromService() {
        val token = UUID.randomUUID().toString()
        val aviseringEpost = "example@example.com"
        every { authService.verifyToken(mockPnr, token) } returns true
        every {
            bolagsverketService.submitSubmission(
                mockPnr,
                mockOrgnr,
                Base64.getDecoder().decode(mockIxbrl),
                aviseringEpost
            )
        } returns InlamningOK().orgnr(mockOrgnr).avsandare(mockPnr)

        val payload =
            """{"foretagOrgnr":"$mockOrgnr","ixbrl":"$mockIxbrl","aviseringEpost":"$aviseringEpost"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(payload)
            .post("/v1/submission-flow/submit")
            .then()
            .statusCode(200)
            .body("orgnr", equalTo(mockOrgnr))
            .body("avsandare", equalTo(mockPnr))
    }
}
