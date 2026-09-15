package com.ibizabroker.bibliotheque.shared.error;

/**
 * Erreur métier traduite en réponse HTTP. Le code est stable (ex.
 * BOOK_UNAVAILABLE) : un client le traduit ou réagit dessus sans analyser le
 * message, qui peut changer ou être traduit.
 */
public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
