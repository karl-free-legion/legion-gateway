package com.zcs.legion.gateway.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateUtils
 * @author lance
 * 1/9/2020 17:12
 */
public final class DateUtils {
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean compare(String date) {
        LocalDateTime dateTime = LocalDateTime.parse(date, FORMATTER);
        return dateTime.compareTo(LocalDateTime.now()) > -1;
    }

    public static long timestamp(String date) {
        return LocalDateTime.parse(date, FORMATTER).toEpochSecond(OffsetDateTime.now().getOffset());
    }
}
