package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        this("NOT_FOUND", message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
