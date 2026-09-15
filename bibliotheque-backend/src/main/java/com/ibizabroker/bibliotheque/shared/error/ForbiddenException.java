package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
