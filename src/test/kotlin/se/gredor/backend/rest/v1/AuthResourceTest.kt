package se.gredor.backend.rest.v1

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import se.gredor.backend.auth.AuthConsts
import se.gredor.backend.auth.AuthService
import java.util.*

@QuarkusTest
class AuthResourceTest {

    val mockPnr = "198605082380"

    @InjectMock
    lateinit var authService: AuthService

    @Test
    fun status_noCookies_returnsLoggedInFalse() {
        // Kör och verifiera
        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/auth/status")
            .then()
            .statusCode(200)
            .body("loggedIn", equalTo(false))
    }

    @Test
    fun status_notAuthenticated_returnsLoggedInFalse() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns false

        // Kör och verifiera
        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(requestJson)
            .post("/v1/auth/status")
            .then()
            .statusCode(200)
            .body("loggedIn", equalTo(false))
    }

    @Test
    fun status_authenticatedWithOtherPnr_returnsLoggedInFalse() {
        val otherPnr = "191212121212"
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(otherPnr, token) } returns true

        // Kör och verifiera
        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$otherPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(requestJson)
            .post("/v1/auth/status")
            .then()
            .statusCode(200)
            .body("loggedIn", equalTo(false))
    }

    @Test
    fun status_authenticatedWithSamePnr_returnsLoggedInTrue() {
        val token = UUID.randomUUID().toString()

        // Mocka
        every { authService.verifyToken(mockPnr, token) } returns true
        
        // Kör och verifiera
        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(requestJson)
            .post("/v1/auth/status")
            .then()
            .statusCode(200)
            .body("loggedIn", equalTo(true))
    }
}
