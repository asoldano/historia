package org.jboss.historia.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Test the web application.
 */
@QuarkusTest
public class WebApplicationTest {

    /**
     * Test that the home page is accessible.
     */
    @Test
    public void testHomePage() {
        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("Historia"));
    }

    /**
     * Test that the API requires authentication.
     */
    @Test
    public void testApiRequiresAuth() {
        given()
            .when().get("/api/requests")
            .then()
                .statusCode(401);
    }

    /**
     * Test that the API works with authentication.
     */
    @Test
    public void testApiWithAuth() {
        given()
            .auth().preemptive().basic("user", "userpassword")
            .when().get("/api/requests")
            .then()
                .statusCode(200);
    }

    /**
     * Test that the requests page requires authentication.
     */
    @Test
    public void testRequestsPageRequiresAuth() {
        given()
            .when().get("/requests")
            .then()
                .statusCode(401);
    }

    /**
     * Test that the requests page works with authentication.
     */
    @Test
    public void testRequestsPageWithAuth() {
        given()
            .auth().preemptive().basic("user", "userpassword")
            .when().get("/requests")
            .then()
                .statusCode(200)
                .body(containsString("Analysis Requests"));
    }
}
