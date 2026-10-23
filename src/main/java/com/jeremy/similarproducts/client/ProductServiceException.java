package com.jeremy.similarproducts.client;

/** Fallo al hablar con las APIs existentes: 5xx, timeout o error de red. */
public class ProductServiceException extends RuntimeException {

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
