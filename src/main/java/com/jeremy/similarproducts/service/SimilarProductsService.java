package com.jeremy.similarproducts.service;

import com.jeremy.similarproducts.client.ProductClient;
import com.jeremy.similarproducts.model.ProductDetail;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimilarProductsService {

    private final ProductClient productClient;

    public SimilarProductsService(ProductClient productClient) {
        this.productClient = productClient;
    }

    /** Versión inicial en serie: una llamada tras otra, conservando el orden de similitud. */
    public List<ProductDetail> findSimilar(String productId) {
        return productClient.findSimilarIds(productId).stream()
                .map(productClient::findById)
                .toList();
    }
}
