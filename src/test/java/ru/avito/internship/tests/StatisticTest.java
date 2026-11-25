package ru.avito.internship.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.api.model.ItemRequest;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.DataFactory;
import ru.avito.internship.support.KnownBug;

class StatisticTest extends ApiTestBase {

    @Test
    @DisplayName("v1: сохранённые счётчики")
    void v1Counters() {
        String id = createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100).statistics(7, 11, 3));

        Response response = api.getStatisticV1(id);

        Checks.assertStatus(response, 200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/statistic.json"));
        assertThat(response.jsonPath().getInt("[0].likes")).isEqualTo(7);
        assertThat(response.jsonPath().getInt("[0].viewCount")).isEqualTo(11);
        assertThat(response.jsonPath().getInt("[0].contacts")).isEqualTo(3);
    }

    @Test
    @DisplayName("v2: сохранённые счётчики")
    void v2Counters() {
        String id = createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100).statistics(5, 9, 2));

        Response response = api.getStatisticV2(id);

        Checks.assertStatus(response, 200);
        assertThat(response.jsonPath().getInt("[0].likes")).isEqualTo(5);
        assertThat(response.jsonPath().getInt("[0].viewCount")).isEqualTo(9);
        assertThat(response.jsonPath().getInt("[0].contacts")).isEqualTo(2);
    }

    @Test
    @DisplayName("v1 и v2 совпадают")
    void versionsMatch() {
        String id = createItem();

        String v1 = api.getStatisticV1(id).asString();
        String v2 = api.getStatisticV2(id).asString();

        assertThat(v2).as("v1 и v2").isEqualTo(v1);
    }

    @Test
    @DisplayName("Несуществующий id")
    void unknownItem() {
        Checks.assertStatus(api.getStatisticV1("00000000-0000-4000-8000-000000000000"), 404);
        Checks.assertStatus(api.getStatisticV2("00000000-0000-4000-8000-000000000000"), 404);
    }

    @Test
    @DisplayName("Не-UUID")
    void malformedId() {
        Checks.assertStatus(api.getStatisticV1("abc"), 400);
    }

    @Test
    @KnownBug(value = "API-02", summary = "статистика живёт после удаления")
    @DisplayName("Статистика после удаления")
    void afterDeletion() {
        String id = createItem();
        api.deleteItem(id);

        Response response = api.getStatisticV1(id);

        Checks.assertStatus(response, 404);
    }

    @Test
    @KnownBug(value = "API-09", summary = "v2 отвечает 404 вместо 400")
    @DisplayName("Код для не-UUID в v1 и v2")
    void sameCodeForMalformedId() {
        int v1 = api.getStatisticV1("abc").statusCode();
        int v2 = api.getStatisticV2("abc").statusCode();

        assertThat(v2).as("код v2").isEqualTo(v1);
    }
}
