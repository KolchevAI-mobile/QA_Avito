package ru.avito.internship.api;

public final class Endpoints {

    public static final String REGISTER = "/api/1/register";
    public static final String AUTHORIZE = "/api/1/autorize";
    public static final String ITEM = "/api/1/item";
    public static final String ITEM_BY_ID = "/api/1/item/{id}";
    public static final String SELLER_ITEMS = "/api/1/{sellerId}/item";
    public static final String STATISTIC_V1 = "/api/1/statistic/{id}";
    public static final String STATISTIC_V2 = "/api/2/statistic/{id}";
    public static final String DELETE_ITEM = "/api/2/item/{id}";

    private Endpoints() {}
}
