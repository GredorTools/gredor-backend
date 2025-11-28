package se.gredor.backend.exceptions

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
class GeneralExceptionMapperTest {

    @InjectMock
    lateinit var service: GeneralExceptionMapperTestService

    @Test
    fun arbitraryException_generatesTechnicalError() {
        // Mocka
        every { service.getSuccess() } throws NullPointerException("Mocked exception")

        // Kör och verifiera
        given()
            .post("/test-rest-controller/general-exception-mapper/test-post")
            .then()
            .statusCode(500)
            .body(containsString("Tekniskt fel"))
    }

    @Test
    fun webApplicationException_handledAsUsual() {
        // Kör och verifiera
        given()
            .get("/test-rest-controller/general-exception-mapper/test-post")
            .then()
            .statusCode(405)
    }

    /**
     * Rest-controller för test.
     */
    @Path("/test-rest-controller/general-exception-mapper/")
    class GeneralExceptionMapperTestRestController {
        lateinit var service: GeneralExceptionMapperTestService

        @POST
        @Path("test-post")
        @Produces(MediaType.APPLICATION_JSON)
        fun testPost(): TestPostResponse {
            val success = service.getSuccess()
            return TestPostResponse(success)
        }

        data class TestPostResponse(val success: Boolean)
    }

    /**
     * Service för test.
     */
    @ApplicationScoped
    class GeneralExceptionMapperTestService {
        fun getSuccess() = true
    }
}
