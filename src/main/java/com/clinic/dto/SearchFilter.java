package com.clinic.dto;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * One query object for every list, report and history endpoint.
 * Each module reads only the fields that apply to it.
 */
@Data
public class SearchFilter {

    /** Upper bound for one page; large enough for a whole busy day's queue (up to 500 patients). */
    private static final int MAX_PAGE_SIZE = 500;

    private String search;
    private String status;
    private String type;
    private Long doctorId;
    private Long patientId;
    private Long roomId;
    private String entityType;
    private Long entityId;
    /** Patients only: leave out patients who are currently admitted (used when admitting). */
    private boolean notAdmitted;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private int page = 0;
    private int size = 20;
    private String sort;

    /** Reports default to the current month when no date range is given. */
    public SearchFilter withDefaultPeriod() {
        if (from == null && to == null) {
            LocalDate today = LocalDate.now();
            from = today.withDayOfMonth(1);
            to = today.withDayOfMonth(today.lengthOfMonth());
        }
        return this;
    }

    /** Uses this ordering when the caller did not ask for one. */
    public SearchFilter withDefaultSort(String defaultSort) {
        if (!hasSort()) {
            sort = defaultSort;
        }
        return this;
    }

    public boolean hasSort() {
        return sort != null && !sort.isBlank();
    }

    /** Page and size only; ordering is supplied by the query itself (e.g. the appointment queue). */
    public Pageable toUnsortedPageable() {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    public Pageable toPageable(String defaultSort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, parseSort(defaultSort));
    }

    /** Accepts "field,dir" or several joined by ';' e.g. "appointmentDate,desc;appointmentTime,asc". */
    private Sort parseSort(String defaultSort) {
        String value = sort == null || sort.isBlank() ? defaultSort : sort;
        return Arrays.stream(value.split(";")).map(SearchFilter::toSort).reduce(Sort.unsorted(), Sort::and);
    }

    private static Sort toSort(String part) {
        String[] pieces = part.split(",");
        boolean ascending = pieces.length > 1 && pieces[1].trim().equalsIgnoreCase("asc");
        return Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, pieces[0].trim());
    }
}
