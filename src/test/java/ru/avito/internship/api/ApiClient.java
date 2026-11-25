package ru.avito.internship.api;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import ru.avito.internship.api.model.Dto;

/** Обёртка над REST Assured, которая повторяет только сетевые сбои и не трогает проверки. */
public final class ApiClient {

    private static final int ATTEMPTS = 3;
    private static final long PAUSE_MS = 700L;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .connectTimeout(Duration.ofSeconds(10))
                    .connectionRequestTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(30)));

    private final String baseUrl;
    private final String token;

    public ApiClient(String baseUrl) {
        this(baseUrl, null);
    }

    public ApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public ApiClient withToken(String newToken) {
        return new ApiClient(baseUrl, newToken);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public RequestSpecification anonymous() {
        return given().baseUri(baseUrl)
                .config(CONFIG)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    public RequestSpecification authorized() {
        RequestSpecification spec = anonymous();
        if (token != null) {
            spec.header("Authorization", "Bearer " + token);
        }
        return spec;
    }

    public Response register(String username, String password) {
        return call(() -> anonymous().body(new Dto.Credentials(username, password)).post(Endpoints.REGISTER), false);
    }

    public Response registerRaw(String rawBody) {
        return call(() -> anonymous().body(rawBody).post(Endpoints.REGISTER), false);
    }

    public Response authorize(String username, String password) {
        return call(() -> anonymous().body(new Dto.Credentials(username, password)).post(Endpoints.AUTHORIZE), false);
    }

    public Response authorizeRaw(String rawBody) {
        return call(() -> anonymous().body(rawBody).post(Endpoints.AUTHORIZE), false);
    }

    public Response createItem(Object body) {
        return call(() -> authorized().body(body).post(Endpoints.ITEM), false);
    }

    public Response getItem(Object id) {
        return call(() -> authorized().pathParam("id", id).get(Endpoints.ITEM_BY_ID), true);
    }

    public Response getSellerItems(Object sellerId) {
        return call(() -> authorized().pathParam("sellerId", sellerId).get(Endpoints.SELLER_ITEMS), true);
    }

    public Response getStatisticV1(Object id) {
        return call(() -> authorized().pathParam("id", id).get(Endpoints.STATISTIC_V1), true);
    }

    public Response getStatisticV2(Object id) {
        return call(() -> authorized().pathParam("id", id).get(Endpoints.STATISTIC_V2), true);
    }

    public Response deleteItem(Object id) {
        return call(() -> authorized().pathParam("id", id).delete(Endpoints.DELETE_ITEM), false);
    }

    public Response get(String path) {
        return call(() -> authorized().get(path), true);
    }

    public Response request(String method, String path) {
        boolean idempotent = "GET".equalsIgnoreCase(method);
        return call(() -> authorized().request(method.toUpperCase(), path), idempotent);
    }

    /**
     * Повтор безопасен, если соединение не установилось: запрос до сервера не дошёл. Остальные сбои
     * повторяем только для идемпотентных методов, иначе POST рискует создать объявление дважды.
     */
    private static Response call(Supplier<Response> request, boolean retryAllowed) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException failure) {
                boolean safeToRetry = isConnectFailure(failure) || retryAllowed;
                if (!safeToRetry || !isTransportFailure(failure) || attempt == ATTEMPTS) {
                    throw failure;
                }
                lastFailure = failure;
                sleep(PAUSE_MS * attempt * 2L);
            }
        }
        throw lastFailure;
    }

    private static boolean isConnectFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getClass().getSimpleName().contains("Connect")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransportFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return true;
            }
            String name = cause.getClass().getSimpleName();
            if (name.contains("Timeout") || name.contains("Connect")) {
                return true;
            }
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }
}
