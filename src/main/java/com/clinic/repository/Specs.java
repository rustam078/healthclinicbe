package com.clinic.repository;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Reusable, null-safe Specification building blocks for filters. A null input yields no condition. */
public final class Specs {

    private Specs() {
    }

    @SafeVarargs
    public static <T> Specification<T> all(Specification<T>... specs) {
        List<Specification<T>> present = Arrays.stream(specs).filter(Objects::nonNull).toList();
        return Specification.allOf(present);
    }

    public static <T> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static <T> Specification<T> eq(String field, Object value) {
        return value == null ? null : (root, query, cb) -> cb.equal(path(root, field), value);
    }

    public static <T> Specification<T> containsAny(String search, String... fields) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(Arrays.stream(fields)
                .map(field -> cb.like(cb.lower(path(root, field).as(String.class)), pattern))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    public static <T> Specification<T> dateBetween(String field, LocalDate from, LocalDate to) {
        return all(from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(path(root, field), from),
                to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(path(root, field), to));
    }

    public static <T> Specification<T> dateTimeBetween(String field, LocalDate from, LocalDate to) {
        return all(from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(path(root, field), from.atStartOfDay()),
                to == null ? null : (root, query, cb) -> cb.lessThan(path(root, field), endOf(to)));
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    @SuppressWarnings("unchecked")
    public static <Y> Path<Y> path(Root<?> root, String field) {
        Path<?> path = root;
        for (String part : field.split("\\.")) {
            path = path.get(part);
        }
        return (Path<Y>) path;
    }
}
