package se.gredor.backend.rest.v1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openapi.quarkus.hamtaArsredovisningsinformation_1_4_yaml.model.Rakenskapsperiod
import se.gredor.backend.bolagsverket.BolagsverketService
import se.gredor.backend.bolagsverket.RecordsResponse
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
        every { bolagsverketService.getRecords(mockOrgnr) } returns
                RecordsResponse(foretagsnamn = "Exempelbolaget AB", rakenskapsperioder = periods)

        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/information/records/$mockOrgnr")
            .then()
            .statusCode(200)
            .body("foretagsnamn", equalTo("Exempelbolaget AB"))
            .body("rakenskapsperioder.size()", equalTo(2))
    }
}
