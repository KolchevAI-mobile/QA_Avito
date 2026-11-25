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
import ru.avito.internship.support.KnownBug;

class ContractTest extends ApiTestBase {

    @Test
    @KnownBug(value = "API-06", summary = "status содержит текст")
    @DisplayName("status совпадает с HTTP-кодом")
    void bodyStatusIsCode() {
        Response response = api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .price(10.5)
                .asMap());

        Checks.assertStatus(response, 400);
        assertThat(response.jsonPath().getString("status"))
                .as("status в теле")
                .isEqualTo("400");
    }

    @Test
    @DisplayName("Ошибки в JSON")
    void errorsAreJson() {
        Checks.assertJsonContentType(anonymousClient().get("/api/1/item/" + ApiClient.randomUuid()));
        Checks.assertJsonContentType(api.getItem("abc"));
    }

}
