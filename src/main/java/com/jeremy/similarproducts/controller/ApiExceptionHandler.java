package com.jeremy.similarproducts.controller;

import com.jeremy.similarproducts.client.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
        // El contrato solo define 404 "Product Not found", sin cuerpo.
    }
}
