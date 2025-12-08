package se.gredor.backend.auth

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.security.UnauthorizedException
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

@QuarkusTest
class AuthFilterTest {

    val mockPnr = "198605082380"

    @InjectMock
    lateinit var authService: AuthService

    @Test
    fun testPost_withoutCookies_returns401() {
        // Kör och verifiera
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .post("/test-rest-controller/auth-filter/test-post")
            .then()
            .statusCode(401)
            .body(containsString("Inloggning saknas"))
    }

    @Test
    fun testPost_withInvalidCookies_returns401() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } throws UnauthorizedException()

        // Kör och verifiera
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .post("/test-rest-controller/auth-filter/test-post")
            .then()
            .statusCode(401)
            .body(containsString("Ogiltig inloggning"))
    }

    @Test
    fun testPost_withValidCookies_success() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true

        // Kör och verifiera
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("personalNumber", mockPnr)
            .cookie("token", token)
            .post("/test-rest-controller/auth-filter/test-post")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
    }

    /**
     * Rest-controller med autentiseringskrav, används endast i AuthFilterTest.
     */
    @Path("/test-rest-controller/auth-filter/")
    @AuthenticationRequired
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
