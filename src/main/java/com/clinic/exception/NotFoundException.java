package com.clinic.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String entityName, Object id) {
        super(HttpStatus.NOT_FOUND, entityName + " not found (id " + id + ")");
    }
}
