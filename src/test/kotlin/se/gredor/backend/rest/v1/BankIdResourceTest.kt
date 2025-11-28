package se.gredor.backend.rest.v1

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
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
    fun init_startsAuth_andReturnsPending() {
        // Mocka
        val response = BankIdStatusResponse(
            orderRef = mockOrderRef,
            autoStartToken = UUID.randomUUID().toString(),
            status = BankIdStatus.PENDING,
            statusPendingData = BankIdStatusPendingData(qrCodeImageBase64 = "data:", hintCode = null)
        )
        every { authService.verifyToken(any(), any()) } throws RuntimeException("Should not be called")
        every { authService.isWithinAuthLimit(mockPnr) } returns true
        every { bankIdService.authInit(mockPnr, any()) } returns response

        // Kör och verifiera
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
        // Mocka
        every { authService.isWithinAuthLimit(mockPnr) } returns false

        // Kör och verifiera
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
        // Mocka
        val response = BankIdStatusResponse(
            status = BankIdStatus.COMPLETE,
            statusCompleteData = BankIdStatusCompleteData(
                personalNumber = mockPnr,
                token = UUID.randomUUID().toString()
            )
        )
        every { bankIdService.authStatus(mockOrderRef) } returns response

        // Kör och verifiera
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
        // Mocka
        every { bankIdService.cancel(mockOrderRef) } returns Unit

        // Kör och verifiera
        val requestJson = """{"orderRef":"$mockOrderRef"}"""
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestJson)
            .post("/v1/bankid/cancel")
            .then()
            .statusCode(200)
    }
}
