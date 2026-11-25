package ru.avito.internship.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.avito.internship.api.ApiClient;
import ru.avito.internship.api.model.ItemRequest;
import ru.avito.internship.config.Config;

@ExtendWith(KnownBugExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    private static final long TEST_PAUSE_MS = 150L;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

    protected ApiClient api;

    protected String username;
    protected long sellerId;
    protected String token;

    private final List<String> itemIdsToDelete = new ArrayList<>();

    @BeforeAll
    void createTestUser() {
        ApiClient anonymous = anonymousClient();
        username = DataFactory.uniqueUsername();

        Response registration = anonymous.register(username, DataFactory.PASSWORD);
        assertThat(registration.statusCode())
                .as("регистрация %s: %s", username, registration.asString())
                .isEqualTo(201);
        sellerId = registration.jsonPath().getLong("id");

        Response authorization = anonymous.authorize(username, DataFactory.PASSWORD);
        assertThat(authorization.statusCode())
                .as("авторизация %s", username)
                .isEqualTo(200);
        token = authorization.jsonPath().getString("accessToken");

        api = new ApiClient(Config.baseUrl(), token);
    }

    @BeforeEach
    void prepareForTest() {
        itemIdsToDelete.clear();
        sleep(TEST_PAUSE_MS);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Убирает созданное в тесте и не валит тест, если стенд недоступен. */
    @AfterEach
    void deleteItemsCreatedByTest() {
        for (String id : itemIdsToDelete) {
            try {
                api.deleteItem(id);
            } catch (Throwable ignored) {
            }
        }
        itemIdsToDelete.clear();
    }

    protected ApiClient anonymousClient() {
        return new ApiClient(Config.baseUrl());
    }

    protected String createItem() {
        return createItem(ItemRequest.valid(sellerId, DataFactory.uniqueItemName(), 1000));
    }

    protected String createItem(ItemRequest request) {
        Response response = api.createItem(request.asMap());
        assertThat(response.statusCode())
                .as("создание: %s", response.asString())
                .isEqualTo(200);
        String id = extractItemId(response);
        itemIdsToDelete.add(id);
        return id;
    }

    /** Достаёт UUID из сообщения вида "Сохранили объявление - <uuid>". */
    protected static String extractItemId(Response response) {
        Matcher matcher = UUID_PATTERN.matcher(response.asString());
        assertThat(matcher.find())
                .as("UUID в ответе: %s", response.asString())
                .isTrue();
        return matcher.group();
    }

    protected void forgetItem(String id) {
        itemIdsToDelete.remove(id);
    }

    /** Убирает объявление, которое сервер создал вопреки ожиданию теста. */
    protected void deleteIfCreated(Response response) {
        if (response.statusCode() == 200) {
            api.deleteItem(extractItemId(response));
        }
    }

    protected void registerForCleanup(String id) {
        itemIdsToDelete.add(id);
    }
}
