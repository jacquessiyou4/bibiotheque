package com.ibizabroker.bibliotheque.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String message) {
        this("SERVICE_UNAVAILABLE", message);
    }

    public ServiceUnavailableException(String code, String message) {
        super(code, message);
    }
}
