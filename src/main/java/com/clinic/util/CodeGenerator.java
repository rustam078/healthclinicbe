package com.clinic.util;

import com.clinic.util.CodeGenerator.CodeType;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Produces human friendly record numbers (PAT-00001, APT-00001, ...) from database sequences. */
@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private final JdbcTemplate jdbcTemplate;

    public String next(CodeType type) {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('" + type.sequence + "')", Long.class);
        return String.format("%s-%05d", type.prefix, value);
    }

    public enum CodeType {
        PATIENT("PAT", "patient_code_seq"),
        APPOINTMENT("APT", "appointment_code_seq"),
        IPD("IPD", "ipd_code_seq");

        private final String prefix;
        private final String sequence;

        CodeType(String prefix, String sequence) {
            this.prefix = prefix;
            this.sequence = sequence;
        }
    }
}
