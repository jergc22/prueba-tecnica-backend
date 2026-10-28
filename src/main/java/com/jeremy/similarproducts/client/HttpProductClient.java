package com.jeremy.similarproducts.client;

import com.jeremy.similarproducts.model.ProductDetail;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
public class HttpProductClient implements ProductClient {

    private static final ParameterizedTypeReference<List<String>> IDS_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpProductClient(RestClient productsRestClient) {
        this.restClient = productsRestClient;
    }

    // Spring no cachea las excepciones: 404, 5xx y timeouts no se guardan.
    // Sin sync = true a propósito: con fallos sin cachear, sync serializaría las peticiones
    // concurrentes a una misma clave y cada una pagaría el timeout completo, una tras otra.
    @Override
    @Cacheable("similarIds")
    public List<String> findSimilarIds(String productId) {
        List<String> ids = call(productId, () -> restClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(IDS_TYPE));
        requireBody(ids, productId);
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .toList();
    }

    @Override
    @Cacheable("products")
    public ProductDetail findById(String productId) {
        ProductDetail product = call(productId, () -> restClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(ProductDetail.class));
        requireBody(product, productId);
        return product;
    }

    /**
     * Traduce 5xx, timeouts, errores de red y respuestas que no cumplen el contrato (JSON inválido,
     * campos obligatorios ausentes) a una excepción propia. El 404 se deja pasar.
     */
    private <T> T call(String productId, Supplier<T> request) {
        try {
            return request.get();
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ProductServiceException("Error calling products API for product " + productId, e);
        }
    }

    private static void requireBody(Object body, String productId) {
        if (body == null) {
            throw new ProductServiceException("Empty response from products API for product " + productId, null);
        }
    }
}
