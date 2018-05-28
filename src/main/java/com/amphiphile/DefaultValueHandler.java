package com.amphiphile;

public class DefaultValueHandler {
    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
