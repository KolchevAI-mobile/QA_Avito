package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.avito.internship.api.model.Dto;
import ru.avito.internship.api.model.ItemRequest;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;
import ru.avito.internship.support.DataFactory;
import ru.avito.internship.support.KnownBug;

class CreateItemTest extends ApiTestBase {

    @Test
    @DisplayName("Создание: статус с UUID")
    void createReturnsUuid() {
        Response response = api.createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 9900).asMap());

        Checks.assertStatus(response, 200);
        Checks.assertJsonContentType(response);
        assertThat(response.jsonPath().getString("status"))
                .as("статус содержит сообщение и UUID")
                .contains("Сохранили объявление");
        registerForCleanup(extractItemId(response));
    }

    @Test
    @DisplayName("Создание и чтение: поля совпадают")
    void itemFieldsRoundTrip() {
        String name = DataFactory.uniqueItemName();
        int price = DataFactory.random(1000, 100000);

        String id = createItem(ItemRequest.valid(sellerId, name, price));
        Response response = api.getItem(id);

        Checks.assertStatus(response, 200);
        Dto.Item item = response.jsonPath().getObject("[0]", Dto.Item.class);
        assertThat(item.id()).as("id").isEqualTo(id);
        assertThat(item.name()).as("name").isEqualTo(name);
        assertThat(item.price()).as("price").isEqualTo(price);
        assertThat(item.sellerId()).as("sellerId").isEqualTo((int) sellerId);
        assertThat(item.statistics().likes()).as("likes").isEqualTo(1);
        assertThat(item.statistics().viewCount()).as("viewCount").isEqualTo(2);
        assertThat(item.statistics().contacts()).as("contacts").isEqualTo(3);
    }

    @Test
    @DisplayName("Параллельное создание")
    void parallelCreation() {
        List<Response> responses = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() ->
                        api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                                .asMap())))
                .map(CompletableFuture::join)
                .toList();

        responses.forEach(response -> Checks.assertStatus(response, 200));
        List<String> ids = responses.stream().map(ApiTestBase::extractItemId).toList();
        ids.forEach(this::registerForCleanup);
        assertThat(ids.stream().distinct().count()).as("уникальных").isEqualTo(5);
    }

    @Test
    @DisplayName("Чужой sellerID")
    void foreignSellerId() {
        long foreignSeller = sellerId + 1000;
        Response response = api.createItem(ItemRequest.valid(foreignSeller, DataFactory.uniqueItemName(), 100)
                .asMap());

        Checks.assertStatus(response, 400);
        assertThat(response.asString()).as("сообщение про sellerID").contains("sellerID");
    }

    @ParameterizedTest(name = "без поля {0}")
    @ValueSource(strings = {"sellerID", "name", "price", "statistics"})
    @DisplayName("Без обязательных полей")
    void requiredFields(String field) {
        ItemRequest request =
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100).without(field);

        Response response = api.createItem(request.asMap());

        Checks.assertStatus(response, 400);
    }

    @ParameterizedTest(name = "price={0}")
    @ValueSource(ints = {1, 999, 100000, Integer.MAX_VALUE})
    @DisplayName("Границы цены")
    void priceBounds(int price) {
        createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), price));
    }

    @Test
    @KnownBug(value = "API-03", summary = "отрицательная цена принимается")
    @DisplayName("Отрицательная цена")
    void negativePrice() {
        Response response = api.createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), -500).asMap());

        deleteIfCreated(response);
        Checks.assertStatus(response, 400);
    }

    @Test
    @KnownBug(value = "API-04", summary = "price=0 считается отсутствующим")
    @DisplayName("Нулевая цена")
    void zeroPrice() {
        Response response = api.createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 0).asMap());

        deleteIfCreated(response);
        Checks.assertStatus(response, 200);
    }

    @Test
    @KnownBug(value = "API-04", summary = "нули считаются отсутствующими")
    @DisplayName("Нулевая статистика")
    void zeroStatistics() {
        Response response = api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .statistics(0, 0, 0)
                .asMap());

        deleteIfCreated(response);
        Checks.assertStatus(response, 200);
    }

    @Test
    @KnownBug(value = "API-03", summary = "отрицательная статистика принимается")
    @DisplayName("Отрицательная статистика")
    void negativeStatistics() {
        Response response = api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .statistics(-1, 2, 3)
                .asMap());

        deleteIfCreated(response);
        Checks.assertStatus(response, 400);
    }

    @Test
    @DisplayName("Неполная статистика")
    void incompleteStatistics() {
        Response response = api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .statistics(Map.of("likes", 1, "viewCount", 2))
                .asMap());

        Checks.assertStatus(response, 400);
        assertThat(response.asString())
                .as("поле в сообщении")
                .contains("contacts");
    }

    @Test
    @DisplayName("Лишний ключ статистики")
    void extraStatisticsKey() {
        createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .statistics(Map.of("likes", 1, "viewCount", 2, "contacts", 3, "shares", 5)));
    }

    @ParameterizedTest(name = "name из {0} символов")
    @ValueSource(ints = {1, 300})
    @DisplayName("Границы длины названия")
    void nameLengths(int length) {
        createItem(ItemRequest.valid(sellerId, "я".repeat(length), 100));
    }

    @Test
    @DisplayName("Пустое название")
    void emptyName() {
        Response response = api.createItem(ItemRequest.valid(sellerId, "", 100).asMap());

        Checks.assertStatus(response, 400);
    }

    @Test
    @KnownBug(value = "API-10", summary = "пробельное название принимается")
    @DisplayName("Название из пробелов")
    void blankName() {
        Response response =
                api.createItem(ItemRequest.valid(sellerId, "    ", 100).asMap());

        deleteIfCreated(response);
        Checks.assertStatus(response, 400);
    }

    @Test
    @KnownBug(value = "API-06", summary = "неверный тип даёт 'не передано тело'")
    @DisplayName("Дробная цена")
    void fractionalPriceMessage() {
        Response response = api.createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100)
                .price(10.5)
                .asMap());

        Checks.assertStatus(response, 400);
        assertThat(response.asString())
                .as("сообщение про price")
                .doesNotContain("не передано тело объявления");
    }

    @Test
    @DisplayName("Лишние поля тела")
    void extraFields() {
        createItem(
                ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 100).with("description", "не из контракта"));
    }
}
