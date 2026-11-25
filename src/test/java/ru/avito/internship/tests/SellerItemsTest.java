package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;

class SellerItemsTest extends ApiTestBase {

    @Test
    @DisplayName("Свои объявления в списке")
    void ownItems() {
        String first = createItem();
        String second = createItem();

        Response response = api.getSellerItems(sellerId);

        Checks.assertStatus(response, 200);
        List<String> ids = response.jsonPath().getList("id", String.class);
        assertThat(ids).as("созданные объявления в списке").contains(first, second);
    }

    @Test
    @DisplayName("Пустой список")
    void emptyList() {
        Response response = api.getSellerItems(999_999_999);

        Checks.assertStatus(response, 200);
        assertThat(response.jsonPath().getList("$")).as("пустой список").isEmpty();
    }

    @Test
    @DisplayName("Нечисловой sellerID")
    void nonNumericSellerId() {
        Response response = api.getSellerItems("abc");

        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Поля элемента списка")
    void itemFields() {
        createItem();

        Map<String, Object> item = api.getSellerItems(sellerId).jsonPath().getList("$").stream()
                .map(value -> (Map<String, Object>) value)
                .filter(map -> map.containsKey("createdAt"))
                .findFirst()
                .orElseThrow();

        assertThat(item)
                .as("поля элемента списка")
                .containsKeys("createdAt", "id", "name", "price", "sellerId", "statistics");
    }
}
