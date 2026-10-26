package com.jeremy.similarproducts.controller;

import com.jeremy.similarproducts.client.ProductNotFoundException;
import com.jeremy.similarproducts.client.ProductServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
        // El contrato solo define 404 "Product Not found", sin cuerpo.
    }

    /** La API de ids no está en el contrato: se responde 502 porque falla un servicio del que dependemos. */
    @ExceptionHandler(ProductServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public void handleUpstreamFailure(ProductServiceException e) {
        log.warn("Upstream failure: {} ({})", e.getMessage(), rootCause(e));
    }

    private static String rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
