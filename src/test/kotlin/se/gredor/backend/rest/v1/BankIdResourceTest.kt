package se.gredor.backend.rest.v1

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import se.gredor.backend.auth.AuthConsts
import se.gredor.backend.auth.AuthService
import se.gredor.backend.bankid.*
import java.util.*

@QuarkusTest
class BankIdResourceTest {

    val mockPnr = "198605082380"

    val mockOrderRef = UUID.randomUUID().toString()

    @InjectMock
    lateinit var bankIdService: BankIdService

    @InjectMock
    lateinit var authService: AuthService

    @Test
    fun init_alreadyAuthenticated_shortCircuits() {
        val token = UUID.randomUUID().toString()
        every { authService.verifyToken(mockPnr, token) } returns true

        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.COOKIE,
                "${AuthConsts.PERSONAL_NUMBER_COOKIE_NAME}=$mockPnr; ${AuthConsts.TOKEN_COOKIE_NAME}=$token"
            )
            .body(requestJson)
            .post("/v1/bankid/init")
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETE"))
            .body("statusCompleteData.personalNumber", equalTo(mockPnr))
            .body("statusCompleteData.token", equalTo(token))
    }

    @Test
    fun init_startsAuth_andReturnsPending() {
        val response = BankIdStatusResponse(
            orderRef = mockOrderRef,
            autoStartToken = UUID.randomUUID().toString(),
            status = AuthStatus.PENDING,
            statusPendingData = StatusPendingData(qrCodeImageBase64 = "data:", hintCode = null)
        )
        every { authService.verifyToken(any(), any()) } throws RuntimeException("Should not be called")
        every { authService.isWithinAuthLimit(mockPnr) } returns true
        every { bankIdService.authInit(mockPnr, any()) } returns response

        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/bankid/init")
            .then()
            .statusCode(200)
            .body("status", equalTo("PENDING"))
            .body("statusPendingData.qrCodeImageBase64", startsWith("data:"))
    }

    @Test
    fun init_aboveAuthLimit_returns400() {
        every { authService.isWithinAuthLimit(mockPnr) } returns false

        val requestJson = """{"personalNumber":"$mockPnr"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/bankid/init")
            .then()
            .statusCode(400)
            .body("error", containsString("Too many authentications"))
    }

    @Test
    fun status_complete_setsCookies() {
        val response = BankIdStatusResponse(
            status = AuthStatus.COMPLETE,
            statusCompleteData = StatusCompleteData(personalNumber = mockPnr, token = UUID.randomUUID().toString())
        )
        every { bankIdService.authStatus(mockOrderRef) } returns response

        val requestJson = """{"orderRef":"$mockOrderRef"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/bankid/status")
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETE"))
            .header("Set-Cookie", any(String::class.java))
    }

    @Test
    fun cancel_success() {
        every { bankIdService.cancel(mockOrderRef) } returns Unit

        val requestJson = """{"orderRef":"$mockOrderRef"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/bankid/cancel")
            .then()
            .statusCode(200)
    }
}
