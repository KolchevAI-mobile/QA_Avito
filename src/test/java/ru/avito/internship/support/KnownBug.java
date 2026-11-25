package ru.avito.internship.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Помечает тест на известный баг, падение которого ожидаемо, а проход валит сборку. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KnownBug {

    String value();

    String summary() default "";
}
