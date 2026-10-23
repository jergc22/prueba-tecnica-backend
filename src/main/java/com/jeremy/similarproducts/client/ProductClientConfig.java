package com.jeremy.similarproducts.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ProductClientConfig {

    @Bean
    RestClient productsRestClient(@Value("${products.api.base-url}") String baseUrl,
                                  @Value("${products.api.connect-timeout}") Duration connectTimeout,
                                  @Value("${products.api.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** Un hilo virtual por tarea: las esperas de red no bloquean hilos de plataforma. */
    @Bean(destroyMethod = "close")
    ExecutorService productsExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
