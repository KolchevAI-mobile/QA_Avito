package ru.avito.internship.api.model;

public final class Dto {

    private Dto() {}

    public record Credentials(String username, String password) {}

    public record User(int id, String username) {}

    public record Token(String accessToken, String tokenType) {}

    public record Statistics(int likes, int viewCount, int contacts) {}

    public record Item(String createdAt, String id, String name, int price, int sellerId, Statistics statistics) {}

    public record CreateItemResult(String status) {}
}
