package com.jeremy.similarproducts.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProductClientConfig {

    @Bean
    RestClient productsRestClient(@Value("${products.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
