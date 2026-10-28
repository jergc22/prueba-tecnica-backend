package com.jeremy.similarproducts.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuración de las APIs existentes ({@code products.api.*} en application.yml). */
@ConfigurationProperties("products.api")
public record ProductsApiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
