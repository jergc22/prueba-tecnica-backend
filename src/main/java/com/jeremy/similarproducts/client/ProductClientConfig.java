package com.jeremy.similarproducts.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableCaching
@EnableConfigurationProperties(ProductsApiProperties.class)
public class ProductClientConfig {

    @Bean
    RestClient productsRestClient(ProductsApiProperties properties) {
        // HTTP/1.1 explícito: evita el intento de upgrade a HTTP/2 (h2c) contra un servidor en http://.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }

    /** Un hilo virtual por tarea: las esperas de red no bloquean hilos de plataforma. */
    @Bean(destroyMethod = "close")
    ExecutorService productsExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
