package se.gredor.backend.rest.v1.resources

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import se.gredor.backend.rest.v1.config.RestConfig
import se.gredor.backend.testutils.RestConfigMockProducer
import java.time.Instant
import java.util.*

@QuarkusTest
class MessageResourceTest {

    @InjectMock
    lateinit var restConfig: RestConfig

    @Test
    fun messages_noMessages_returnsEmptyList() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        every { restConfig.messages() } returns Optional.of(emptyList())

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/message/messages")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }

    @Test
    fun messages_withUntimedMessages_returnsMessages() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        every { restConfig.messages() } returns Optional.of(
            listOf(
                object : RestConfig.Message {
                    override fun text() = "Första roliga meddelandet!"
                    override fun startTime() = Optional.empty<Instant>()
                    override fun endTime() = Optional.empty<Instant>()
                },
                object : RestConfig.Message {
                    override fun text() = "Andra, mindre roliga meddelandet."
                    override fun startTime() = Optional.empty<Instant>()
                    override fun endTime() = Optional.empty<Instant>()
                }
            )
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/message/messages")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].text", equalTo("Första roliga meddelandet!"))
            .body("[1].text", equalTo("Andra, mindre roliga meddelandet."))
    }

    @Test
    fun messages_withTimedMessages_returnsCorrectMessages() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        every { restConfig.messages() } returns Optional.of(
            listOf(
                object : RestConfig.Message {
                    override fun text() = "Bara startTime, ska visas"
                    override fun startTime() = Optional.of(Instant.parse("2005-01-20T12:34:56+01:00"))
                    override fun endTime() = Optional.empty<Instant>()
                },
                object : RestConfig.Message {
                    override fun text() = "Bara endTime, ska visas"
                    override fun startTime() = Optional.empty<Instant>()
                    override fun endTime() = Optional.of(Instant.parse("2905-01-20T12:34:56+01:00"))
                },
                object : RestConfig.Message {
                    override fun text() = "Både startTime och endTime, ska visas"
                    override fun startTime() = Optional.of(Instant.parse("2005-01-20T12:34:56+01:00"))
                    override fun endTime() = Optional.of(Instant.parse("2905-01-20T12:34:56+01:00"))
                },
                object : RestConfig.Message {
                    override fun text() = "Bara startTime, ska INTE visas"
                    override fun startTime() = Optional.of(Instant.parse("2905-01-20T12:34:56+01:00"))
                    override fun endTime() = Optional.empty<Instant>()
                },
                object : RestConfig.Message {
                    override fun text() = "Bara endTime, ska INTE visas"
                    override fun startTime() = Optional.empty<Instant>()
                    override fun endTime() = Optional.of(Instant.parse("2005-01-20T12:34:56+01:00"))
                },
                object : RestConfig.Message {
                    override fun text() = "Både startTime och endTime i framtiden, ska INTE visas"
                    override fun startTime() = Optional.of(Instant.parse("2805-01-20T12:34:56+01:00"))
                    override fun endTime() = Optional.of(Instant.parse("2905-01-20T12:34:56+01:00"))
                },
                object : RestConfig.Message {
                    override fun text() = "Både startTime och endTime i dåtiden, ska INTE visas"
                    override fun startTime() = Optional.of(Instant.parse("2005-01-20T12:34:56+01:00"))
                    override fun endTime() = Optional.of(Instant.parse("2006-01-20T12:34:56+01:00"))
                }
            )
        )

        // Kör och verifiera
        given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/v1/message/messages")
            .then()
            .statusCode(200)
            .body("size()", equalTo(3))
            .body("[0].text", equalTo("Bara startTime, ska visas"))
            .body("[1].text", equalTo("Bara endTime, ska visas"))
            .body("[2].text", equalTo("Både startTime och endTime, ska visas"))
    }
}
