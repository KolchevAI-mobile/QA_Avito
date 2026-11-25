package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.avito.internship.api.model.ItemRequest;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.DataFactory;

class AccessTest extends ApiTestBase {

    private static final String ANY_UUID = "00000000-0000-4000-8000-000000000000";

    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/1/item/" + ANY_UUID),
                Arguments.of("GET", "/api/1/1/item"),
                Arguments.of("GET", "/api/1/statistic/" + ANY_UUID),
                Arguments.of("GET", "/api/2/statistic/" + ANY_UUID),
                Arguments.of("POST", "/api/1/item"),
                Arguments.of("DELETE", "/api/2/item/" + ANY_UUID));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    @DisplayName("Без заголовка Authorization")
    void noToken(String method, String path) {
        Response response = callAnonymous(method, path);

        Checks.assertStatus(response, 401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    @DisplayName("Несуществующий токен")
    void garbageToken(String method, String path) {
        Response response = callAnonymous(method, path, "Authorization", "Bearer garbage.token.value");

        Checks.assertStatus(response, 401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    @DisplayName("Другая схема авторизации")
    void otherAuthScheme(String method, String path) {
        Response response = callAnonymous(method, path, "Authorization", "Basic dXNlcjpwYXNz");

        Checks.assertStatus(response, 401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    @DisplayName("Пустой Bearer")
    void emptyBearer(String method, String path) {
        Response response = callAnonymous(method, path, "Authorization", "Bearer");

        Checks.assertStatus(response, 401);
    }

    @Test
    @DisplayName("Подмена через X-UserId")
    void xUserIdCannotSpoof() {
        String name = DataFactory.uniqueItemName();
        ItemRequest request = ItemRequest.valid(sellerId + 1, name, 1000);

        Response response = api.authorized()
                .header("X-UserId", String.valueOf(sellerId + 1))
                .body(request.asMap())
                .post("/api/1/item");

        Checks.assertStatus(response, 400);
        assertThat(response.asString()).as("сообщение про sellerID").contains("sellerID");
    }

    @Test
    @DisplayName("Валидный токен")
    void validToken() {
        Response response = api.get("/api/1/item/" + ANY_UUID);

        assertThat(response.statusCode())
                .as("код с валидным токеном")
                .isNotEqualTo(401);
    }

    private Response callAnonymous(String method, String path) {
        return anonymousClient().anonymous().request(method.toUpperCase(), path);
    }

    private Response callAnonymous(String method, String path, String headerName, String headerValue) {
        return anonymousClient().anonymous().header(headerName, headerValue).request(method.toUpperCase(), path);
    }
}
