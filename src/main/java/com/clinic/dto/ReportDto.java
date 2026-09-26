package com.clinic.dto;

import com.clinic.entity.Appointment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Report payload shared by Appointment and IPD reports (and summarised on the Dashboard).
 *
 * @param summary   counts by status plus TOTAL
 * @param breakdown counts by a secondary dimension (doctor, type, room type)
 * @param trend     counts per day (ISO date keys, ascending)
 * @param amounts   informational totals (estimates only; nothing is billed)
 * @param rows      the paged detail rows
 */
public record ReportDto<T>(Map<String, Long> summary,
                           Map<String, Map<String, Long>> breakdown,
                           Map<String, Long> trend,
                           Map<String, BigDecimal> amounts,
                           PageResponse<T> rows) {
}
