package com.formula1.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_FORMATTER);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return format(dateTime, DateTimeFormatter.ofPattern(pattern));
    }

    private static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(formatter);
    }
}
