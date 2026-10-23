package com.jeremy.similarproducts.client;

import com.jeremy.similarproducts.model.ProductDetail;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class HttpProductClient implements ProductClient {

    private static final ParameterizedTypeReference<List<String>> IDS_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpProductClient(RestClient productsRestClient) {
        this.restClient = productsRestClient;
    }

    @Override
    public List<String> findSimilarIds(String productId) {
        return restClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(IDS_TYPE);
    }

    @Override
    public ProductDetail findById(String productId) {
        return restClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(ProductDetail.class);
    }
}
