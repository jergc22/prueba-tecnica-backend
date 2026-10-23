package com.jeremy.similarproducts.client;

import com.jeremy.similarproducts.model.ProductDetail;

import java.util.List;

/**
 * Puerto hacia las APIs existentes. El servicio depende de esta interfaz y no
 * de RestClient (inversión de dependencias).
 */
public interface ProductClient {

    /** Ids de productos similares, ordenados por similitud. */
    List<String> findSimilarIds(String productId);

    ProductDetail findById(String productId);
}
