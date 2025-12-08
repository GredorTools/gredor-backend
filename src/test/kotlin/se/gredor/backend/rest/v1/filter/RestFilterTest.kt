package se.gredor.backend.rest.v1.filter

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import se.gredor.backend.rest.v1.config.PerResourceString
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.testutils.RestConfigMockProducer
import java.util.*

@QuarkusTest
class RestFilterTest {

    @InjectMock
    lateinit var restConfig: RestConfig

    @Test
    fun testPost_nothingClosed_returns200() {
        RestConfigMockProducer.createDefaultMocks(restConfig)

        // Kör och verifiera
        given()
            .post("/test-rest-controller/rest-filter/test-post")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
    }

    @Test
    fun testPost_resourceClosed_returns503() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        val perResourceStrings: RestConfig.PerResourceStrings = object : RestConfig.PerResourceStrings {
            override fun auth() = Optional.empty<String>()
            override fun bankId() = Optional.of("Tjänsten är stängd")
            override fun information() = Optional.empty<String>()
            override fun message() = Optional.empty<String>()
            override fun ping() = Optional.empty<String>()
            override fun submissionFlow() = Optional.empty<String>()
        }
        every { restConfig.closedResources() } returns Optional.of(perResourceStrings)

        // Kör och verifiera
        given()
            .post("/test-rest-controller/rest-filter/test-post")
            .then()
            .statusCode(503)
            .body(equalTo("Tjänsten är stängd"))
    }

    @Test
    fun testPost_otherResourcesClosed_returns200() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        val perResourceStrings: RestConfig.PerResourceStrings = object : RestConfig.PerResourceStrings {
            override fun auth() = Optional.of("Tjänsten är stängd")
            override fun bankId() = Optional.empty<String>()
            override fun information() = Optional.of("Tjänsten är stängd")
            override fun message() = Optional.of("Tjänsten är stängd")
            override fun ping() = Optional.of("Tjänsten är stängd")
            override fun submissionFlow() = Optional.of("Tjänsten är stängd")
        }
        every { restConfig.closedResources() } returns Optional.of(perResourceStrings)

        // Kör och verifiera
        given()
            .post("/test-rest-controller/rest-filter/test-post")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
    }

    /**
     * Rest-controller med autentiseringskrav, används endast i AuthFilterTest.
     */
    @Path("/test-rest-controller/rest-filter/")
    @GredorRestResource(PerResourceString.BANK_ID)
    class AuthFilterTestRestController {
        @POST
        @Path("test-post")
        @Produces(MediaType.APPLICATION_JSON)
        fun testPost(): TestPostResponse {
            return TestPostResponse(true)
        }

        data class TestPostResponse(val success: Boolean)
    }

}
