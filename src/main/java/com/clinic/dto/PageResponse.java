package com.clinic.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Paged list payload shared by all list and report endpoints. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
