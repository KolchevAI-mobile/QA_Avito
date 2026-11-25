package ru.avito.internship.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;

public final class Checks {

    private Checks() {}

    public static void assertStatus(Response response, int expected) {
        assertThat(response.statusCode())
                .as("HTTP %d, получили %d: %s", expected, response.statusCode(), response.asString())
                .isEqualTo(expected);
    }

    public static void assertJsonContentType(Response response) {
        assertThat(response.contentType()).as("content-type").containsIgnoringCase("application/json");
    }

    public static void assertEmptyBody(Response response) {
        assertThat(response.asString())
                .as("тело удаления")
                .isEmpty();
    }
}
