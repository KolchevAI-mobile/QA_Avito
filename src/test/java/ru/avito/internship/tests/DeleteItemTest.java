package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.KnownBug;

class DeleteItemTest extends ApiTestBase {

    @Test
    @DisplayName("Удаление владельцем")
    void ownerDeletes() {
        String id = createItem();

        Response response = api.deleteItem(id);

        Checks.assertStatus(response, 200);
        Checks.assertEmptyBody(response);
        Checks.assertStatus(api.getItem(id), 404);
        forgetItem(id);
    }

    @Test
    @DisplayName("Не-UUID при удалении")
    void malformedId() {
        Checks.assertStatus(api.deleteItem("abc"), 400);
    }

    @Test
    @KnownBug(value = "API-01", summary = "чужой токен удаляет чужое объявление")
    @DisplayName("Удаление чужого объявления")
    void foreignItem() {
        String id = createItem();
        String anotherUser = "another_" + System.nanoTime();
        var anonymous = anonymousClient();
        Checks.assertStatus(anonymous.register(anotherUser, "strong-password"), 201);
        String anotherToken =
                anonymous.authorize(anotherUser, "strong-password").jsonPath().getString("accessToken");

        Response response = api.withToken(anotherToken).deleteItem(id);

        assertThat(response.statusCode()).as("код удаления").isIn(403, 404);
        Checks.assertStatus(api.getItem(id), 200);
    }

    @Test
    @KnownBug(value = "API-05", summary = "в теле 500 при HTTP 404")
    @DisplayName("Повторное удаление")
    void repeatedDeleteStatus() {
        String id = createItem();
        api.deleteItem(id);
        forgetItem(id);

        Response response = api.deleteItem(id);

        Checks.assertStatus(response, 404);
        assertThat(response.jsonPath().getString("status"))
                .as("status в теле")
                .isEqualTo("404");
    }

    @Test
    @KnownBug(value = "API-05", summary = "в теле 500 при HTTP 404")
    @DisplayName("Удаление несуществующего")
    void unknownDeleteStatus() {
        Response response = api.deleteItem("00000000-0000-4000-8000-000000000000");

        Checks.assertStatus(response, 404);
        assertThat(response.jsonPath().getString("status"))
                .as("status в теле")
                .isEqualTo("404");
    }
}
