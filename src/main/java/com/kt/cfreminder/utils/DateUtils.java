package com.kt.cfreminder.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateUtils {


    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public String getCurrentDateTimeThai(String timezone) {

        ZoneId zone = ZoneId.of(timezone);

        ZonedDateTime now = ZonedDateTime.now(zone);

        ThaiBuddhistDate thaiDate = ThaiBuddhistDate.from(now);

        String date = DATE_FORMAT.format(thaiDate);
        String time = TIME_FORMAT.format(now);

        return date + " " + time;
    }

    public String formatBuddhistDate(LocalDate date) {
        return DATE_FORMAT.format(ThaiBuddhistDate.from(date));
    }
}
