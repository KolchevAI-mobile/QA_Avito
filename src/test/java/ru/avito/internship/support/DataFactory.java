package ru.avito.internship.support;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import net.datafaker.Faker;

public final class DataFactory {

    public static final String PASSWORD = "strong-password";

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Faker FAKER = new Faker(new Locale("ru"));

    private DataFactory() {}

    public static String uniqueUsername() {
        return "qa_" + System.currentTimeMillis() + "_" + COUNTER.incrementAndGet() + "_" + random(100, 999);
    }

    public static String uniqueItemName() {
        String base = FAKER.commerce().productName().replaceAll("\\s+", " ");
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        return base + " #" + random(1000, 9999);
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
