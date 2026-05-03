package com.foody.controller;

import com.foody.dto.OrderStatusRequest;
import com.foody.dto.PlaceOrderRequest;
import com.foody.model.Order;
import com.foody.model.OrderStatus;
import com.foody.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public Order placeOrder(@RequestBody(required = false) PlaceOrderRequest request,
                            Authentication auth) {
        return orderService.placeOrder(auth.getName(), request);
    }

    @GetMapping
    public List<Order> myOrders(Authentication auth) {
        return orderService.getUserOrders(auth.getName());
    }

    @PutMapping("/{orderId}/cancel")
    public Order cancelMine(@PathVariable String orderId,
                            Authentication auth) {
        return orderService.cancelCustomerOrder(auth.getName(), orderId);
    }

    @GetMapping(value = "/{orderId}/invoice", produces = MediaType.TEXT_PLAIN_VALUE)
    public String invoice(@PathVariable String orderId,
                          Authentication auth) {
        return orderService.invoiceForCustomer(auth.getName(), orderId);
    }

    @GetMapping("/admin/all")
    public List<Order> allOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/admin/revenue")
    public double totalRevenue() {
        return orderService.getTotalRevenue();
    }

    @PutMapping("/admin/{orderId}/cancel")
    public Order adminCancel(@PathVariable String orderId) {
        return orderService.cancelAdminOrder(orderId);
    }

    @GetMapping(value = "/admin/{orderId}/invoice", produces = MediaType.TEXT_PLAIN_VALUE)
    public String adminInvoice(@PathVariable String orderId) {
        return orderService.invoiceForAdmin(orderId);
    }

    @GetMapping("/admin/analytics")
    public Map<String, Object> analytics() {
        return orderService.getAnalytics();
    }

    @PutMapping("/admin/{orderId}/status")
    public Order updateStatus(@PathVariable String orderId,
                              @Valid @RequestBody OrderStatusRequest request) {
        OrderStatus status = OrderStatus.valueOf(request.getStatus().toUpperCase());
        return orderService.updateOrderStatus(orderId, status);
    }
}
