package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
