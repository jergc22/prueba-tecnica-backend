package com.jeremy.similarproducts.service;

import com.jeremy.similarproducts.client.ProductClient;
import com.jeremy.similarproducts.client.ProductNotFoundException;
import com.jeremy.similarproducts.client.ProductServiceException;
import com.jeremy.similarproducts.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductClient productClient;
    private final ExecutorService executor;

    public SimilarProductsService(ProductClient productClient, ExecutorService productsExecutor) {
        this.productClient = productClient;
        this.executor = productsExecutor;
    }

    /**
     * Pide los detalles en paralelo y conserva el orden de similitud. Si un similar falla
     * (404, 5xx, timeout o respuesta inválida) se omite y se devuelve el resto (respuesta parcial).
     * Si falla la consulta de ids, la excepción se propaga: 404 o 502 según el caso.
     */
    public List<ProductDetail> findSimilar(String productId) {
        // distinct: el contrato exige elementos únicos; conserva el orden de la primera aparición.
        List<String> similarIds = productClient.findSimilarIds(productId).stream()
                .distinct()
                .toList();

        List<CompletableFuture<ProductDetail>> futures = similarIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> fetchOrNull(id), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    // Solo se capturan los fallos esperados de la API: un error de programación no se esconde.
    private ProductDetail fetchOrNull(String id) {
        try {
            return productClient.findById(id);
        } catch (ProductNotFoundException e) {
            log.debug("Omitting similar product {}: not found", id);
            return null;
        } catch (ProductServiceException e) {
            log.warn("Omitting similar product {}: {}", id, e.rootCauseDescription());
            return null;
        }
    }
}
