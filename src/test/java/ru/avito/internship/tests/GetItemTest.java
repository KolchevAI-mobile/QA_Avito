package ru.avito.internship.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.restassured.response.Response;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.api.ApiClient;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.KnownBug;

class GetItemTest extends ApiTestBase {

    @Test
    @DisplayName("Массив из одного элемента")
    void singleElementArray() {
        String id = createItem();

        Response response = api.getItem(id);

        Checks.assertStatus(response, 200);
        Checks.assertJsonContentType(response);
        assertThat(response.jsonPath().getList("$")).as("массив").hasSize(1);
        assertThat(response.jsonPath().getString("[0].id")).isEqualTo(id);
    }

    @Test
    @DisplayName("Схема ответа")
    void matchesSchema() {
        String id = createItem();

        api.getItem(id).then().statusCode(200).body(matchesJsonSchemaInClasspath("schemas/item.json"));
    }

    @Test
    @DisplayName("Несуществующий id")
    void unknownItem() {
        String id = ApiClient.randomUuid();

        Response response = api.getItem(id);

        Checks.assertStatus(response, 404);
        assertThat(response.asString()).as("id в сообщении").contains(id);
    }

    @Test
    @DisplayName("Не-UUID")
    void malformedId() {
        Response response = api.getItem("123");

        Checks.assertStatus(response, 400);
        assertThat(response.asString()).as("сообщение о причине").containsIgnoringCase("uuid");
    }

    @Test
    @DisplayName("UUID в верхнем регистре")
    void uppercaseUuid() {
        String id = createItem();

        Response response = api.getItem(id.toUpperCase());

        Checks.assertStatus(response, 200);
    }

    @Test
    @KnownBug(value = "API-07", summary = "смещение задвоено")
    @DisplayName("Формат createdAt")
    void createdAtFormat() {
        String id = createItem();

        String createdAt = api.getItem(id).jsonPath().getString("[0].createdAt");

        assertThatCode(() -> OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .as("createdAt: %s", createdAt)
                .doesNotThrowAnyException();
    }
}
