package ru.curs.asciidoctor_idgen;

public class DefaultValueHandler {
    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
