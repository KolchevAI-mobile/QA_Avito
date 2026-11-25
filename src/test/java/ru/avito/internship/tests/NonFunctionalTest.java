package ru.avito.internship.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.internship.config.Config;
import ru.avito.internship.support.ApiTestBase;
import ru.avito.internship.support.Checks;

class NonFunctionalTest extends ApiTestBase {

    @Test
    @DisplayName("Время ответа")
    void responseTime() {
        String id = createItem();

        Response response = api.getItem(id);

        Checks.assertStatus(response, 200);
        assertThat(response.time())
                .as("время ответа, мс (лимит %d)", Config.maxResponseTimeMs())
                .isLessThan(Config.maxResponseTimeMs());
    }

    @Test
    @DisplayName("Content-Type")
    void contentType() {
        String id = createItem();

        Checks.assertJsonContentType(api.getItem(id));
        Checks.assertJsonContentType(api.getStatisticV1(id));
        Checks.assertJsonContentType(api.getSellerItems(sellerId));
    }

    @Test
    @DisplayName("Повторное чтение")
    void repeatedRead() {
        String id = createItem();

        String first = api.getItem(id).asString();
        String second = api.getItem(id).asString();

        assertThat(second).as("ответы").isEqualTo(first);
    }

    @Test
    @DisplayName("Параллельные чтения")
    void parallelReads() {
        String id = createItem();

        List<Integer> codes = IntStream.range(0, 10)
                .mapToObj(
                        i -> CompletableFuture.supplyAsync(() -> api.getItem(id).statusCode()))
                .map(CompletableFuture::join)
                .toList();

        assertThat(codes).as("коды").containsOnly(200);
    }
}
