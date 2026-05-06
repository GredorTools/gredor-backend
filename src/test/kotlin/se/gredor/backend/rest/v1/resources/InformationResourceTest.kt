package se.gredor.backend.rest.v1.resources

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod
import org.openapi.quarkus.lamnaInArsredovisning_2_1_yaml.model.Fel
import se.gredor.backend.bolagsverket.BolagsverketRecordsResponse
import se.gredor.backend.bolagsverket.BolagsverketService
import java.time.LocalDate

@QuarkusTest
@ExtendWith(MockKExtension::class)
class InformationResourceTest {

    val mockOrgnr = "5569999999"

    @InjectMock
    lateinit var bolagsverketService: BolagsverketService

    @Test
    fun records_returnsDataFromService() {
        val periods = listOf(
            Rakenskapsperiod().from(LocalDate.of(2023, 1, 1)).tom(LocalDate.of(2023, 12, 31)),
            Rakenskapsperiod().from(LocalDate.of(2024, 1, 1)).tom(LocalDate.of(2024, 12, 31)),
        )

        // Mocka
        every { bolagsverketService.getRecords(mockOrgnr) } returns
                BolagsverketRecordsResponse(
                    foretagsnamn = "Exempelbolaget AB",
                    rakenskapsperioder = periods,
                    harVerkstallandeDirektor = true,
                    harLikvidator = false,
                )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(200)
            .body("foretagsnamn", equalTo("Exempelbolaget AB"))
            .body("rakenskapsperioder.size()", equalTo(2))
            .body("harVerkstallandeDirektor", equalTo(true))
            .body("harLikvidator", equalTo(false))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_on9004Fel() {
        // Mocka
        val fel = Fel().kod(9004).text("Riktigt obra")
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(fel).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_returnsCustomError_on4005Fel() {
        // Mocka
        val fel = Fel().kod(4005).text("Ingen träff på efterfrågat organisationsnummer.")
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(fel).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(400)
            .contentType(MediaType.TEXT_PLAIN)
            .body(equalTo("Ingen träff på efterfrågat organisationsnummer. Observera att Gredor endast har stöd för aktiebolag."))
    }

    @Test
    fun exceptionMapper_forwardsFelText_onOtherFelCodes() {
        // Mocka
        val felText = "Exempelfel, något gick obra!"
        val fel = Fel().kod(1234).text(felText)
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(fel).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(400)
            .contentType(MediaType.TEXT_PLAIN)
            .body(equalTo(felText))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_onNonFelEntity() {
        // Mocka
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).type(MediaType.APPLICATION_JSON).entity(mapOf("foo" to "bar")).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_on400WithoutEntity() {
        // Mocka
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(400).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun exceptionMapper_returnsTechnicalError_onNon400Status() {
        // Mocka
        every { bolagsverketService.getRecords(mockOrgnr) } throws ClientWebApplicationException(
            Response.status(401).build()
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(500)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString("Tekniskt fel"))
    }
}
