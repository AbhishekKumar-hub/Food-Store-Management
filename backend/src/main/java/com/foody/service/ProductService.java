package com.foody.service;

import com.foody.model.Product;
import com.foody.repository.ProductRepository;
import com.foody.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository productRepository,
                          ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    public Product create(Product product) {

        if (product.getName() == null || product.getName().isBlank()) {
            throw new RuntimeException("Product name is required");
        }

        if (product.getPrice() < 0) {
            throw new RuntimeException("Price cannot be negative");
        }

        int stock = Math.max(product.getStock(), 0);
        product.setStock(stock);
        product.setAvailable(stock > 0);


        product.setName(product.getName().trim());
        product.setDescription(clean(product.getDescription()));
        product.setCategory(cleanOrDefault(product.getCategory(), "Meals"));
        product.setImageUrl(clean(product.getImageUrl()));
        product.setPreparationTimeMinutes(Math.max(product.getPreparationTimeMinutes(), 5));

        return attachRatingSummary(productRepository.save(product));
    }


    public Product update(String id, Product updated) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (updated.getName() == null || updated.getName().isBlank()) {
            throw new RuntimeException("Product name is required");
        }

        if (updated.getPrice() < 0) {
            throw new RuntimeException("Price cannot be negative");
        }

        product.setName(updated.getName().trim());
        product.setDescription(clean(updated.getDescription()));
        product.setCategory(cleanOrDefault(updated.getCategory(), "Meals"));
        product.setPrice(updated.getPrice());
        product.setImageUrl(clean(updated.getImageUrl()));
        product.setVegetarian(updated.isVegetarian());
        product.setPreparationTimeMinutes(Math.max(updated.getPreparationTimeMinutes(), 5));

        int stock = Math.max(updated.getStock(), 0);
        product.setStock(stock);
        product.setAvailable(stock > 0);

        return attachRatingSummary(productRepository.save(product));
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }


    public List<Product> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::attachRatingSummary)
                .toList();
    }

    public Product getById(String id) {
        return attachRatingSummary(productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found")));
    }


    public synchronized void reduceStock(String productId, int quantity) {

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isAvailable() || product.getStock() <= 0) {
            throw new RuntimeException(product.getName() + " is out of stock");
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException(
                    "Only " + product.getStock() + " item(s) left for " + product.getName()
            );
        }

        product.setStock(product.getStock() - quantity);
        product.setAvailable(product.getStock() > 0);

        productRepository.save(product);
    }


    public List<Product> searchFilterSort(
            String keyword,
            Boolean available,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String direction,
            String category,
            Boolean vegetarian
    ) {
        List<Product> products;

        if (keyword != null && !keyword.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            products = productRepository.findAll();
        }

        if (available != null) {
            products = products.stream()
                    .filter(p -> p.isAvailable() == available)
                    .toList();
        }

        if (minPrice != null) {
            products = products.stream()
                    .filter(p -> p.getPrice() >= minPrice)
                    .toList();
        }

        if (maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getPrice() <= maxPrice)
                    .toList();
        }

        if (category != null && !category.isBlank()) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                    .toList();
        }

        if (vegetarian != null) {
            products = products.stream()
                    .filter(p -> p.isVegetarian() == vegetarian)
                    .toList();
        }

        if (sortBy != null) {
            switch (sortBy) {
                case "price" -> products = products.stream()
                        .sorted(direction != null && direction.equalsIgnoreCase("desc")
                                ? Comparator.comparing(Product::getPrice).reversed()
                                : Comparator.comparing(Product::getPrice))
                        .toList();
                case "name" -> products = products.stream()
                        .sorted(direction != null && direction.equalsIgnoreCase("desc")
                                ? Comparator.comparing(Product::getName).reversed()
                                : Comparator.comparing(Product::getName))
                        .toList();
                case "stock" -> products = products.stream()
                        .sorted(direction != null && direction.equalsIgnoreCase("desc")
                                ? Comparator.comparing(Product::getStock).reversed()
                                : Comparator.comparing(Product::getStock))
                        .toList();
            }
        }

        return products.stream()
                .map(this::attachRatingSummary)
                .toList();
    }

    public List<String> getCategories() {
        return productRepository.findAll()
                .stream()
                .map(Product::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<Product> getFeatured() {
        return productRepository.findAll()
                .stream()
                .filter(product -> product.isAvailable() && product.getStock() > 0)
                .map(this::attachRatingSummary)
                .sorted(Comparator.comparing(Product::getReviewCount).reversed()
                        .thenComparing(Product::getAverageRating, Comparator.reverseOrder())
                        .thenComparing(Product::getStock, Comparator.reverseOrder()))
                .limit(8)
                .toList();
    }

    public List<Product> seedProducts() {
        List<Product> existing = productRepository.findAll();
        List<String> existingNames = existing.stream()
                .map(product -> product.getName() == null ? "" : product.getName().toLowerCase())
                .toList();

        List<Product> samples = new ArrayList<>();
        samples.add(sample("Margherita Pizza", "Pizza", 149, 18, true, "Cheesy classic pizza with tomato basil sauce.", "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?auto=format&fit=crop&w=900&q=80"));
        samples.add(sample("Paneer Tikka Bowl", "Meals", 179, 14, true, "Smoky paneer, rice, salad and mint dip.", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=900&q=80"));
        samples.add(sample("Chicken Biryani", "Biryani", 199, 12, false, "Aromatic biryani with raita and salad.", "https://images.unsplash.com/photo-1633945274405-b6c8069047b0?auto=format&fit=crop&w=900&q=80"));
        samples.add(sample("Masala Dosa", "South Indian", 89, 24, true, "Crisp dosa with potato masala and chutneys.", "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=900&q=80"));
        samples.add(sample("Chocolate Brownie", "Desserts", 79, 20, true, "Warm brownie with rich chocolate flavour.", "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=900&q=80"));

        samples.stream()
                .filter(product -> !existingNames.contains(product.getName().toLowerCase()))
                .forEach(this::create);

        return getAll();
    }

    private Product attachRatingSummary(Product product) {
        List<com.foody.model.Review> reviews = reviewRepository.findByProductId(product.getId());
        int count = reviews.size();
        double average = count == 0
                ? 0
                : reviews.stream().mapToInt(com.foody.model.Review::getRating).average().orElse(0);

        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            final int currentRating = rating;
            breakdown.put(rating, reviews.stream().filter(review -> review.getRating() == currentRating).count());
        }

        product.setReviewCount(count);
        product.setAverageRating(Math.round(average * 10.0) / 10.0);
        product.setRatingBreakdown(breakdown);
        return product;
    }

    private Product sample(String name, String category, double price, int stock, boolean vegetarian, String description, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setVegetarian(vegetarian);
        product.setDescription(description);
        product.setImageUrl(imageUrl);
        product.setPreparationTimeMinutes(25);
        return product;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
