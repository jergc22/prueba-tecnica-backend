package com.jeremy.similarproducts.service;

import com.jeremy.similarproducts.client.ProductClient;
import com.jeremy.similarproducts.client.ProductNotFoundException;
import com.jeremy.similarproducts.client.ProductServiceException;
import com.jeremy.similarproducts.model.ProductDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilarProductsServiceTest {

    private ProductClient client;
    private ExecutorService executor;
    private SimilarProductsService service;

    @BeforeEach
    void setUp() {
        client = mock(ProductClient.class);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new SimilarProductsService(client, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void keepsSimilarityOrderEvenWhenResponsesArriveOutOfOrder() {
        when(client.findSimilarIds("1")).thenReturn(List.of("2", "3", "4"));
        when(client.findById("2")).thenAnswer(inv -> {
            Thread.sleep(200);
            return product("2");
        });
        when(client.findById("3")).thenReturn(product("3"));
        when(client.findById("4")).thenAnswer(inv -> {
            Thread.sleep(100);
            return product("4");
        });

        List<ProductDetail> result = service.findSimilar("1");

        assertThat(result).extracting(ProductDetail::id).containsExactly("2", "3", "4");
    }

    @Test
    void omitsSimilarProductsThatFail() {
        when(client.findSimilarIds("1")).thenReturn(List.of("2", "3", "4", "5"));
        when(client.findById("2")).thenReturn(product("2"));
        when(client.findById("3")).thenThrow(new ProductNotFoundException("3"));
        when(client.findById("4")).thenThrow(new ProductServiceException("boom", new RuntimeException()));
        when(client.findById("5")).thenReturn(product("5"));

        List<ProductDetail> result = service.findSimilar("1");

        assertThat(result).extracting(ProductDetail::id).containsExactly("2", "5");
    }

    @Test
    void returnsEmptyListWhenThereAreNoSimilarIds() {
        when(client.findSimilarIds("1")).thenReturn(List.of());

        assertThat(service.findSimilar("1")).isEmpty();
    }

    @Test
    void propagatesNotFoundWhenTheProductDoesNotExist() {
        when(client.findSimilarIds(anyString())).thenThrow(new ProductNotFoundException("99"));

        assertThatThrownBy(() -> service.findSimilar("99")).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void propagatesUpstreamFailureWhenSimilarIdsFail() {
        when(client.findSimilarIds("1")).thenThrow(new ProductServiceException("boom", new RuntimeException()));

        assertThatThrownBy(() -> service.findSimilar("1")).isInstanceOf(ProductServiceException.class);
    }

    private static ProductDetail product(String id) {
        return new ProductDetail(id, "Product " + id, new BigDecimal("9.99"), true);
    }
}
