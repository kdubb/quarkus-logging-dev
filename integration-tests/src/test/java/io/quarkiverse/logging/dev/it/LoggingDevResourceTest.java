package io.quarkiverse.logging.dev.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingDevResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/logging-dev")
                .then()
                .statusCode(200)
                .body(is("Hello logging-dev"));
    }
}
