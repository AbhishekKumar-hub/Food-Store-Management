package com.foody.controller;

import com.foody.dto.AdminUserResponse;
import com.foody.model.Product;
import com.foody.service.OrderService;
import com.foody.service.ProductService;
import com.foody.service.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    public AdminController(OrderService orderService,
                           ProductService productService,
                           UserService userService) {
        this.orderService = orderService;
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboardStats() {

        Map<String, Object> analytics = orderService.getAnalytics();
        return Map.of(
                "totalRevenue", orderService.getTotalRevenue(),
                "totalOrders", orderService.getAllOrders().size(),
                "totalProducts", productService.getAll().size(),
                "analytics", analytics
        );
    }

    @PostMapping("/seed-products")
    public List<Product> seedProducts() {
        return productService.seedProducts();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return userService.getUsersForAdmin();
    }

    @PutMapping("/users/{userId}/block")
    public AdminUserResponse blockUser(@PathVariable String userId,
                                       @RequestParam boolean blocked) {
        return userService.setBlocked(userId, blocked);
    }
}
