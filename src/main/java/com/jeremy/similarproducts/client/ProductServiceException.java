package com.jeremy.similarproducts.client;

/** Fallo al hablar con las APIs existentes: 5xx, timeout, error de red o respuesta inválida. */
public class ProductServiceException extends RuntimeException {

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Clase y mensaje de la causa raíz, para que el log diga qué falló de verdad (timeout, 500...). */
    public String rootCauseDescription() {
        Throwable cause = this;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause == this ? getMessage() : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
