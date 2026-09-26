package com.clinic.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import jakarta.persistence.criteria.Predicate;

/** Grouped counts and sums over any entity, filtered with the same Specifications as the lists. */
@Component
@RequiredArgsConstructor
public class ReportQueries {

    private final EntityManager entityManager;

    public <E> Map<String, Long> countBy(Class<E> type, Specification<E> spec, String field) {
        return group(type, spec, (root, cb) -> Specs.path(root, field));
    }

    /** Counts grouped by a field plus a TOTAL entry, used for report summary cards. */
    public <E> Map<String, Long> countWithTotal(Class<E> type, Specification<E> spec, String field) {
        Map<String, Long> counts = countBy(type, spec, field);
        counts.put("TOTAL", counts.values().stream().mapToLong(Long::longValue).sum());
        return counts;
    }

    /** Counts per calendar day of a timestamp column. */
    public <E> Map<String, Long> countByDay(Class<E> type, Specification<E> spec, String field) {
        return group(type, spec, (root, cb) -> cb.function("date", LocalDate.class, Specs.path(root, field)));
    }

    public <E> BigDecimal sum(Class<E> type, Specification<E> spec, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<E> root = query.from(type);
        query.select(cb.sum(Specs.<BigDecimal>path(root, field)));
        restrict(query, spec.toPredicate(root, query, cb));
        BigDecimal total = entityManager.createQuery(query).getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }

    private <E> Map<String, Long> group(Class<E> type, Specification<E> spec,
                                        BiFunction<Root<E>, CriteriaBuilder, Expression<?>> keyOf) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(type);
        Expression<?> key = keyOf.apply(root, cb);
        query.multiselect(key, cb.count(root)).groupBy(key).orderBy(cb.asc(key));
        restrict(query, spec.toPredicate(root, query, cb));
        return toMap(entityManager.createQuery(query).getResultList());
    }

    private void restrict(CriteriaQuery<?> query, Predicate predicate) {
        if (predicate != null) {
            query.where(predicate);
        }
    }

    private Map<String, Long> toMap(List<Tuple> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(String.valueOf(row.get(0)), row.get(1, Long.class)));
        return result;
    }
}
