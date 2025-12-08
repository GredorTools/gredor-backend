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
    fun messages_withMessages_returnsMessages() {
        // Mocka
        RestConfigMockProducer.createDefaultMocks(restConfig)
        every { restConfig.messages() } returns Optional.of(
            listOf(
                object : RestConfig.Message {
                    override fun text() = "Första roliga meddelandet!"
                },
                object : RestConfig.Message {
                    override fun text() = "Andra, mindre roliga meddelandet."
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
}
