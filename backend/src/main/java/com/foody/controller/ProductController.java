package com.foody.controller;

import com.foody.model.Product;
import com.foody.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @RequestBody Product product) {
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        productService.delete(id);
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @GetMapping("/featured")
    public List<Product> featured() {
        return productService.getFeatured();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable String id) {
        return productService.getById(id);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean vegetarian
    ) {
        return productService.searchFilterSort(
                keyword,
                available,
                minPrice,
                maxPrice,
                sortBy,
                direction,
                category,
                vegetarian
        );
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.getCategories();
    }
}
