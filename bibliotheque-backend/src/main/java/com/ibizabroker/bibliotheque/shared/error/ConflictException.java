package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ConflictException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        this("CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
