package com.foody.service;

import com.foody.model.CartItem;
import com.foody.model.Order;
import com.foody.model.Product;
import com.foody.repository.OrderRepository;
import com.foody.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ChatbotService(ProductRepository productRepository,
                          OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> reply(String message, String userEmail) {
        String lowerMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
        List<Product> recommendations = recommendationsFor(userEmail);
        String reply;

        if (containsAny(lowerMessage, "recommend", "popular", "best", "most ordered", "suggest")) {
            reply = "Based on recent orders, these dishes are a good pick today.";
        } else if (containsAny(lowerMessage, "delivery", "time", "late")) {
            reply = "Most orders move from placed to preparing to rider on the way. You can track the latest status in Orders.";
        } else if (containsAny(lowerMessage, "payment", "razorpay", "upi", "card", "cod")) {
            reply = "You can pay using Cash on Delivery. Razorpay appears automatically after test keys are configured.";
        } else if (containsAny(lowerMessage, "address", "location", "map")) {
            reply = "Add your saved address from Profile or use the location button during checkout to pin your delivery point.";
        } else if (containsAny(lowerMessage, "refund", "cancel")) {
            reply = "For this college project, cancellation and refunds are handled by the admin from the order dashboard.";
        } else {
            reply = "I can help with delivery status, payments, addresses, reviews, and dish recommendations.";
        }

        return Map.of(
                "reply", reply,
                "recommendations", recommendations
        );
    }

    private List<Product> recommendationsFor(String userEmail) {
        List<Product> availableProducts = productRepository.findAll()
                .stream()
                .filter(product -> product.isAvailable() && product.getStock() > 0)
                .toList();

        if (userEmail != null && !userEmail.isBlank()) {
            List<Order> userOrders = orderRepository.findByUserEmail(userEmail);
            Map<String, Long> orderedProductCounts = userOrders.stream()
                    .filter(order -> order.getItems() != null)
                    .flatMap(order -> order.getItems().stream())
                    .collect(Collectors.groupingBy(CartItem::getProductId, LinkedHashMap::new, Collectors.counting()));

            List<Product> personal = availableProducts.stream()
                    .filter(product -> orderedProductCounts.containsKey(product.getId()))
                    .sorted(Comparator.comparing((Product product) -> orderedProductCounts.get(product.getId())).reversed())
                    .limit(4)
                    .toList();

            if (!personal.isEmpty()) {
                return personal;
            }
        }

        return availableProducts.stream()
                .sorted(Comparator.comparing(Product::getStock).reversed())
                .limit(4)
                .toList();
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
