package com.clinic.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class Dates {

    private Dates() {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** "26 Sep 2026" for messages and the timeline. */
    public static String display(LocalDate date) {
        return date == null ? "" : DAY.format(date);
    }

    /** "10:30 AM" for messages and the timeline. */
    public static String display(LocalTime time) {
        return time == null ? "" : TIME.format(time);
    }

    public static Integer age(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Age in words suited to a children's clinic: "12 days", "5 months 18 days", "1 year 6 months", "7 years".
     */
    public static String ageText(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        Period age = Period.between(dateOfBirth, LocalDate.now());
        if (age.getYears() >= 5) {
            return unit(age.getYears(), "year");
        }
        return age.getYears() >= 1 ? join(unit(age.getYears(), "year"), unit(age.getMonths(), "month"))
                : age.getMonths() >= 1 ? join(unit(age.getMonths(), "month"), unit(age.getDays(), "day"))
                : unit(Math.max(age.getDays(), 0), "day");
    }

    private static String unit(int value, String word) {
        return value + " " + word + (value == 1 ? "" : "s");
    }

    private static String join(String main, String rest) {
        return rest.startsWith("0 ") ? main : main + " " + rest;
    }

    /** Calendar days of stay, counting a same-day stay as one day. */
    public static long stayDays(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            return 0;
        }
        LocalDateTime end = to == null ? LocalDateTime.now() : to;
        return Math.max(1, ChronoUnit.DAYS.between(from.toLocalDate(), end.toLocalDate()));
    }
}
