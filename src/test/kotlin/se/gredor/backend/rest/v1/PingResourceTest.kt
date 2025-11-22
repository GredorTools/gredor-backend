package se.gredor.backend.rest.v1

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class PingResourceTest {

    @Test
    fun ping_returnsPong() {
        given()
            .get("/v1/ping/ping")
            .then()
            .statusCode(200)
            .body(equalTo("pong"))
    }
}
