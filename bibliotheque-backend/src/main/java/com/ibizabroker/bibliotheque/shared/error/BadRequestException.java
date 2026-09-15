package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends ApiException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        this("BAD_REQUEST", message);
    }

    public BadRequestException(String code, String message) {
        super(code, message);
    }
}
