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

class RegisterTest extends ApiTestBase {

    @Test
    @DisplayName("Успешная регистрация")
    void newUser() {
        String login = DataFactory.uniqueUsername();

        Response response = anonymousClient().register(login, DataFactory.PASSWORD);

        Checks.assertStatus(response, 201);
        Checks.assertJsonContentType(response);
        assertThat(response.jsonPath().getLong("id"))
                .as("id")
                .isPositive();
        assertThat(response.jsonPath().getString("username"))
                .as("username")
                .isEqualTo(login);
    }

    @Test
    @DisplayName("Занятый username")
    void duplicateUsername() {
        Response response = anonymousClient().register(username, DataFactory.PASSWORD);

        Checks.assertStatus(response, 409);
        assertThat(response.asString()).as("сообщение").containsIgnoringCase("registered");
    }

    @Test
    @DisplayName("Уникальные id")
    void uniqueIds() {
        Response first = anonymousClient().register(DataFactory.uniqueUsername(), DataFactory.PASSWORD);
        Response second = anonymousClient().register(DataFactory.uniqueUsername(), DataFactory.PASSWORD);

        Checks.assertStatus(first, 201);
        Checks.assertStatus(second, 201);
        assertThat(first.jsonPath().getLong("id"))
                .as("id")
                .isNotEqualTo(second.jsonPath().getLong("id"));
    }

    @ParameterizedTest(name = "username={0}, password={1} -> 400")
    @ValueSource(strings = {"", "ab", "a"})
    @DisplayName("Короткий username")
    void shortUsername(String shortUsername) {
        Response response = anonymousClient().register(shortUsername, DataFactory.PASSWORD);

        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Длинный username")
    void longUsername() {
        Response response = anonymousClient().register("q".repeat(200), DataFactory.PASSWORD);

        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Границы длины username")
    void usernameLengthBounds() {
        String sixtyFour = ("q" + System.nanoTime()).concat("x".repeat(64)).substring(0, 64);

        Checks.assertStatus(registerThreeCharUsername(), 201);
        Checks.assertStatus(anonymousClient().register(sixtyFour, DataFactory.PASSWORD), 201);
    }

    /** Пробует несколько трёхсимвольных имён, потому что такое имя может быть уже занято. */
    private Response registerThreeCharUsername() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        Response last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                name.append(alphabet.charAt(DataFactory.random(0, alphabet.length() - 1)));
            }
            last = anonymousClient().register(name.toString(), DataFactory.PASSWORD);
            if (last.statusCode() != 409) {
                return last;
            }
        }
        return last;
    }

    @ParameterizedTest(name = "password={0}")
    @ValueSource(strings = {"", "1", "1234567"})
    @DisplayName("Короткий пароль")
    void shortPassword(String shortPassword) {
        Response response = anonymousClient().register(DataFactory.uniqueUsername(), shortPassword);

        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Без обязательных полей")
    void missingFields() {
        Checks.assertStatus(
                anonymousClient().registerRaw("{\"username\":\"" + DataFactory.uniqueUsername() + "\"}"), 400);
        Checks.assertStatus(anonymousClient().registerRaw("{\"password\":\"strong-password\"}"), 400);
        Checks.assertStatus(anonymousClient().registerRaw("{}"), 400);
    }

    @Test
    @DisplayName("Тип username")
    void wrongType() {
        Response response = anonymousClient().registerRaw("{\"username\":12345,\"password\":\"strong-password\"}");

        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Битый JSON")
    void brokenJson() {
        Response response = anonymousClient().registerRaw("{not a json");

        Checks.assertStatus(response, 400);
        Checks.assertJsonContentType(response);
    }

    @Test
    @DisplayName("Лишние поля тела")
    void extraFields() {
        String body = "{\"username\":\"" + DataFactory.uniqueUsername()
                + "\",\"password\":\"strong-password\",\"role\":\"admin\"}";

        Response response = anonymousClient().registerRaw(body);

        Checks.assertStatus(response, 201);
    }

    @Test
    @DisplayName("Username из пробелов")
    void blankUsername() {
        Response response = anonymousClient().register("   ", DataFactory.PASSWORD);

        Checks.assertStatus(response, 400);
    }

    @Test
    @KnownBug(value = "API-08", summary = "401 вместо 405")
    @DisplayName("Метод на /register")
    void methodNotAllowed() {
        Response response = anonymousClient().get("/api/1/register");

        Checks.assertStatus(response, 405);
    }

    @Test
    @KnownBug(value = "API-11", summary = "сообщение про JSON вместо длины")
    @DisplayName("Сообщение о коротком username")
    void shortUsernameMessage() {
        Response response = anonymousClient().register("ab", DataFactory.PASSWORD);

        assertThat(response.jsonPath().getString("error"))
                .as("сообщение про длину")
                .doesNotContain("invalid JSON body");
    }
}
