package ru.avito.internship.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Тело запроса с полями типа Object, потому что часть тестов отправляет неверные типы. */
public final class ItemRequest {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static ItemRequest valid(long sellerId, String name, int price) {
        ItemRequest request = new ItemRequest();
        request.fields.put("sellerID", sellerId);
        request.fields.put("name", name);
        request.fields.put("price", price);
        request.fields.put("statistics", Map.of("likes", 1, "viewCount", 2, "contacts", 3));
        return request;
    }

    public static ItemRequest empty() {
        return new ItemRequest();
    }

    public ItemRequest sellerId(Object value) {
        fields.put("sellerID", value);
        return this;
    }

    public ItemRequest name(Object value) {
        fields.put("name", value);
        return this;
    }

    public ItemRequest price(Object value) {
        fields.put("price", value);
        return this;
    }

    public ItemRequest statistics(Object value) {
        fields.put("statistics", value);
        return this;
    }

    public ItemRequest statistics(int likes, int viewCount, int contacts) {
        return statistics(Map.of("likes", likes, "viewCount", viewCount, "contacts", contacts));
    }

    public ItemRequest without(String field) {
        fields.remove(field);
        return this;
    }

    public ItemRequest with(String field, Object value) {
        fields.put(field, value);
        return this;
    }

    public Map<String, Object> asMap() {
        return Map.copyOf(fields);
    }
}
