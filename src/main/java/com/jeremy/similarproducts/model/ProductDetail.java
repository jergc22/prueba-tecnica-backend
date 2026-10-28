package com.jeremy.similarproducts.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Detalle de producto según el contrato: todos los campos son obligatorios y id y name no
 * pueden estar vacíos. Si la API existente devuelve un detalle incompleto, no se construye.
 */
public record ProductDetail(String id, String name, BigDecimal price, Boolean availability) {

    public ProductDetail {
        requireNotBlank(id, "id");
        requireNotBlank(name, "name");
        Objects.requireNonNull(price, "price is required");
        Objects.requireNonNull(availability, "availability is required");
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
