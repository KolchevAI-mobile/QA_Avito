package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.api.ApiClient;
import ru.avito.internship.api.model.ItemRequest;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.DataFactory;

class EndToEndTest extends ApiTestBase {

    @Test
    @DisplayName("Полный цикл объявления")
    void fullItemLifecycle() {
        ApiClient client = anonymousClient();
        String login = DataFactory.uniqueUsername();
        String name = DataFactory.uniqueItemName();

        Response registration = client.register(login, DataFactory.PASSWORD);
        Checks.assertStatus(registration, 201);
        long owner = registration.jsonPath().getLong("id");

        Response authorization = client.authorize(login, DataFactory.PASSWORD);
        Checks.assertStatus(authorization, 200);
        String accessToken = authorization.jsonPath().getString("accessToken");

        ApiClient authorized = client.withToken(accessToken);
        Response creation = authorized.createItem(
                ItemRequest.valid(owner, name, 15000).statistics(2, 4, 1).asMap());
        Checks.assertStatus(creation, 200);
        String itemId = extractItemId(creation);
        registerForCleanup(itemId);

        Response item = authorized.getItem(itemId);
        Checks.assertStatus(item, 200);
        assertThat(item.jsonPath().getString("[0].name")).as("название").isEqualTo(name);
        assertThat(item.jsonPath().getInt("[0].price")).as("цена").isEqualTo(15000);

        Response statisticV1 = authorized.getStatisticV1(itemId);
        Checks.assertStatus(statisticV1, 200);
        assertThat(statisticV1.jsonPath().getInt("[0].viewCount"))
                .as("просмотры v1")
                .isEqualTo(4);

        Response statisticV2 = authorized.getStatisticV2(itemId);
        Checks.assertStatus(statisticV2, 200);
        assertThat(statisticV2.jsonPath().getInt("[0].viewCount"))
                .as("просмотры v2")
                .isEqualTo(4);

        Checks.assertStatus(authorized.deleteItem(itemId), 200);
        Checks.assertStatus(authorized.getItem(itemId), 404);
    }
}
