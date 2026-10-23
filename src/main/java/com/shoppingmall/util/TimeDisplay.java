package com.shoppingmall.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("timeDisplay")
public class TimeDisplay {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public String format(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONE).format(FORMAT);
    }
}
