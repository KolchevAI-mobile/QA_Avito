package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.DataFactory;
import ru.avito.internship.support.KnownBug;

class AuthorizeTest extends ApiTestBase {

    @Test
    @DisplayName("Успешная авторизация")
    void successfulAuthorization() {
        Response response = anonymousClient().authorize(username, DataFactory.PASSWORD);

        Checks.assertStatus(response, 200);
        Checks.assertJsonContentType(response);
        assertThat(response.jsonPath().getString("accessToken"))
                .as("accessToken")
                .isNotBlank();
        assertThat(response.jsonPath().getString("tokenType")).as("tokenType").isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Токен открывает ручку")
    void tokenOpensEndpoint() {
        String itemId = createItem();

        Response response = api.getItem(itemId);

        Checks.assertStatus(response, 200);
    }

    @Test
    @DisplayName("Неверный пароль")
    void wrongPassword() {
        Response response = anonymousClient().authorize(username, "wrong-password-123");

        Checks.assertStatus(response, 401);
    }

    @Test
    @DisplayName("Неизвестный пользователь")
    void unknownUser() {
        Response response =
                anonymousClient().authorize("no_such_" + DataFactory.uniqueUsername(), DataFactory.PASSWORD);

        Checks.assertStatus(response, 401);
    }

    @Test
    @DisplayName("Без пароля")
    void missingPassword() {
        Checks.assertStatus(anonymousClient().authorizeRaw("{\"username\":\"" + username + "\"}"), 400);
        Checks.assertStatus(anonymousClient().authorizeRaw("{}"), 400);
    }

    @Test
    @DisplayName("Регистр username")
    void caseSensitiveUsername() {
        Response response = anonymousClient().authorize(username.toUpperCase(), DataFactory.PASSWORD);

        Checks.assertStatus(response, 401);
    }

    @Test
    @DisplayName("Пароль не попадает в ответ")
    void noPasswordInResponse() {
        Response response = anonymousClient().authorize(username, DataFactory.PASSWORD);

        assertThat(response.asString()).as("пароль в ответе").doesNotContain(DataFactory.PASSWORD);
        assertThat(response.jsonPath().getMap("$").keySet())
                .as("поля ответа")
                .containsExactlyInAnyOrder("accessToken", "tokenType");
    }

    @ParameterizedTest(name = "password={0}")
    @ValueSource(strings = {"", "1", "1234567"})
    @DisplayName("Короткий пароль")
    void shortPassword(String shortPassword) {
        Response response = anonymousClient().authorize(username, shortPassword);

        Checks.assertStatus(response, 400);
    }

    @Test
    @KnownBug(value = "API-11", summary = "сообщение про JSON вместо длины")
    @DisplayName("Сообщение о коротком пароле")
    void shortPasswordMessage() {
        Response response = anonymousClient().authorize(username, "123");

        assertThat(response.asString())
                .as("сообщение про длину")
                .doesNotContain("invalid JSON body");
    }
}
