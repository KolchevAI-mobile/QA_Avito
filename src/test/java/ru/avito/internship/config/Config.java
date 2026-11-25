package ru.avito.internship.config;

/** Адрес стенда можно переопределить через -DbaseUrl или переменную BASE_URL. */
public final class Config {

    private static final String DEFAULT_BASE_URL = "https://qa-internship.avito.com";

    private Config() {}

    public static String baseUrl() {
        String fromProperty = System.getProperty("baseUrl");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return trimTrailingSlash(fromProperty);
        }
        String fromEnv = System.getenv("BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return trimTrailingSlash(fromEnv);
        }
        return DEFAULT_BASE_URL;
    }

    public static int maxResponseTimeMs() {
        return 3000;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
