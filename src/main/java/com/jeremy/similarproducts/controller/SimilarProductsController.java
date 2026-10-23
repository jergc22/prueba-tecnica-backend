package com.jeremy.similarproducts.controller;

import com.jeremy.similarproducts.model.ProductDetail;
import com.jeremy.similarproducts.service.SimilarProductsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SimilarProductsController {

    private final SimilarProductsService service;

    public SimilarProductsController(SimilarProductsService service) {
        this.service = service;
    }

    @GetMapping("/product/{productId}/similar")
    public List<ProductDetail> getSimilar(@PathVariable String productId) {
        return service.findSimilar(productId);
    }
}
