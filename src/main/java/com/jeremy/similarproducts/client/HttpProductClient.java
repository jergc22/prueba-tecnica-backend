package com.jeremy.similarproducts.client;

import com.jeremy.similarproducts.model.ProductDetail;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.function.Supplier;

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
        return call(productId, () -> restClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(IDS_TYPE));
    }

    @Override
    public ProductDetail findById(String productId) {
        return call(productId, () -> restClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(ProductDetail.class));
    }

    /** Traduce 5xx, timeouts y errores de red a una excepción propia; el 404 se deja pasar. */
    private <T> T call(String productId, Supplier<T> request) {
        try {
            return request.get();
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ProductServiceException("Error calling products API for product " + productId, e);
        }
    }
}
