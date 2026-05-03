package com.foody.service;

import com.foody.dto.PlaceOrderRequest;
import com.foody.model.Address;
import com.foody.model.Cart;
import com.foody.model.CartItem;
import com.foody.model.Order;
import com.foody.model.OrderStatus;
import com.foody.model.Product;
import com.foody.model.RestaurantInfo;
import com.foody.model.User;
import com.foody.repository.CartRepository;
import com.foody.repository.OrderRepository;
import com.foody.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final RestaurantInfoService restaurantInfoService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        ProductRepository productRepository,
                        UserService userService,
                        RestaurantInfoService restaurantInfoService,
                        NotificationService notificationService,
                        PaymentService paymentService,
                        CouponService couponService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.restaurantInfoService = restaurantInfoService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.couponService = couponService;
    }

    public synchronized Order placeOrder(String email, PlaceOrderRequest request) {
        PlaceOrderRequest orderRequest = request == null ? new PlaceOrderRequest() : request;

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        User user = userService.getUserByEmail(email);
        Address deliveryAddress = resolveDeliveryAddress(email, orderRequest);
        String paymentMode = normalizePaymentMode(orderRequest.getPaymentMode());

        List<Product> productsToUpdate = new ArrayList<>();
        Map<String, Product> productById = new LinkedHashMap<>();

        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!product.isAvailable() || product.getStock() <= 0) {
                throw new RuntimeException(product.getName() + " is out of stock");
            }

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException(
                        "Only " + product.getStock() + " item(s) left for " + product.getName()
                );
            }

            productsToUpdate.add(product);
            productById.put(product.getId(), product);
        }

        if ("RAZORPAY".equals(paymentMode)) {
            boolean verified = paymentService.verifyPayment(
                    orderRequest.getRazorpayOrderId(),
                    orderRequest.getRazorpayPaymentId(),
                    orderRequest.getRazorpaySignature()
            );

            if (!verified) {
                throw new RuntimeException("Payment verification failed. Please try again.");
            }
        }

        for (int i = 0; i < productsToUpdate.size(); i++) {
            Product product = productsToUpdate.get(i);
            CartItem item = cart.getItems().get(i);

            product.setStock(product.getStock() - item.getQuantity());
            product.setAvailable(product.getStock() > 0);
            productRepository.save(product);
        }

        List<CartItem> orderItems = new ArrayList<>();
        double subtotalAmount = 0;

        for (CartItem item : cart.getItems()) {
            Product product = productById.get(item.getProductId());
            CartItem snapshot = new CartItem();
            snapshot.setProductId(item.getProductId());
            snapshot.setName(product.getName());
            snapshot.setImageUrl(product.getImageUrl());
            snapshot.setCategory(product.getCategory());
            snapshot.setPrice(product.getPrice());
            snapshot.setQuantity(item.getQuantity());

            orderItems.add(snapshot);
            subtotalAmount += snapshot.getTotalPrice();
        }

        com.foody.dto.CouponValidationResponse couponResult =
                couponService.validate(orderRequest.getCouponCode(), subtotalAmount);
        boolean couponApplied = orderRequest.getCouponCode() != null
                && !orderRequest.getCouponCode().isBlank()
                && couponResult.isValid();
        if (orderRequest.getCouponCode() != null && !orderRequest.getCouponCode().isBlank() && !couponResult.isValid()) {
            throw new RuntimeException(couponResult.getMessage());
        }
        double discountAmount = couponApplied ? couponResult.getDiscountAmount() : 0;
        double totalAmount = Math.max(subtotalAmount - discountAmount, 0);

        LocalDateTime now = LocalDateTime.now();
        RestaurantInfo restaurant = restaurantInfoService.getRestaurant();

        Order order = new Order();
        order.setUserEmail(email);
        order.setCustomerName(user.getName());
        order.setCustomerPhone(user.getPhone());
        order.setItems(orderItems);
        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setCouponCode(couponApplied ? couponResult.getCode() : "");
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus("RAZORPAY".equals(paymentMode) ? "PAID" : "COD_PENDING");
        order.setRazorpayOrderId(orderRequest.getRazorpayOrderId());
        order.setRazorpayPaymentId(orderRequest.getRazorpayPaymentId());
        order.setDeliveryAddress(deliveryAddress);
        order.setRestaurant(restaurant);
        order.setOrderNote(clean(orderRequest.getOrderNote()));
        order.setOrderTime(now);
        order.setUpdatedAt(now);
        order.setStatus(OrderStatus.PLACED);
        order.getStatusTimeline().put(OrderStatus.PLACED.name(), now);

        cartRepository.delete(cart);

        Order savedOrder = orderRepository.save(order);
        notificationService.sendOrderPlacedEmail(user, savedOrder);
        return savedOrder;
    }

    public List<Order> getUserOrders(String email) {
        return orderRepository.findByUserEmail(email)
                .stream()
                .sorted(Comparator.comparing(Order::getOrderTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Order::getOrderTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public double getTotalRevenue() {
        return orderRepository.findAll()
                .stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    public Order updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (status == OrderStatus.CANCELLED) {
            return cancelAdminOrder(orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled orders cannot be updated");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(status);
        order.setUpdatedAt(now);
        order.getStatusTimeline().put(status.name(), now);

        Order savedOrder = orderRepository.save(order);
        if (status == OrderStatus.DELIVERED) {
            notificationService.sendOrderDeliveredEmail(savedOrder);
        }
        return savedOrder;
    }

    public Order updateOrderStatus(String orderId) {
        return updateOrderStatus(orderId, OrderStatus.DELIVERED);
    }

    public Order cancelCustomerOrder(String email, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserEmail().equals(email)) {
            throw new RuntimeException("You can cancel only your own orders");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        if (order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order cannot be cancelled after preparation starts");
        }

        return cancelOrder(order);
    }

    public Order cancelAdminOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Delivered orders cannot be cancelled");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        return cancelOrder(order);
    }

    public String invoiceForCustomer(String email, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUserEmail().equals(email)) {
            throw new RuntimeException("Invoice not found");
        }
        return invoiceText(order);
    }

    public String invoiceForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return invoiceText(order);
    }

    public Map<String, Object> getAnalytics() {
        List<Order> orders = orderRepository.findAll();
        List<Product> products = productRepository.findAll();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orders.stream().filter(order -> order.getStatus() == status).count();
            statusCounts.put(status.name(), count);
        }

        Map<String, Double> revenueByDay = revenueByDay(orders);
        Map<String, Double> categoryRevenue = categoryRevenue(orders);
        List<Map<String, Object>> topProducts = topProducts(orders);
        long lowStockCount = products.stream().filter(product -> product.getStock() > 0 && product.getStock() <= 5).count();

        return Map.of(
                "totalRevenue", getTotalRevenue(),
                "totalOrders", orders.size(),
                "pendingOrders", statusCounts.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals(OrderStatus.DELIVERED.name())
                                && !entry.getKey().equals(OrderStatus.CANCELLED.name()))
                        .mapToLong(Map.Entry::getValue)
                        .sum(),
                "deliveredOrders", statusCounts.get(OrderStatus.DELIVERED.name()),
                "lowStockProducts", lowStockCount,
                "statusCounts", statusCounts,
                "revenueByDay", revenueByDay,
                "categoryRevenue", categoryRevenue,
                "topProducts", topProducts
        );
    }

    private Address resolveDeliveryAddress(String email, PlaceOrderRequest request) {
        Address requestAddress = request.getDeliveryAddress();
        if (requestAddress != null && requestAddress.getLine1() != null && !requestAddress.getLine1().isBlank()) {
            return requestAddress;
        }
        return userService.findDeliveryAddress(email, request.getAddressId());
    }

    private String normalizePaymentMode(String paymentMode) {
        if (paymentMode != null && paymentMode.equalsIgnoreCase("RAZORPAY")) {
            return "RAZORPAY";
        }
        return "CASH_ON_DELIVERY";
    }

    private Order cancelOrder(Order order) {
        restoreStock(order);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(now);
        order.getStatusTimeline().put(OrderStatus.CANCELLED.name(), now);

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_REQUIRED");
        }

        return orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        if (order.isStockRestored() || order.getItems() == null) {
            return;
        }

        for (CartItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                product.setAvailable(product.getStock() > 0);
                productRepository.save(product);
            });
        }

        order.setStockRestored(true);
    }

    private String invoiceText(Order order) {
        StringBuilder builder = new StringBuilder();
        builder.append("FOODY INVOICE\n");
        builder.append("Order ID: ").append(order.getId()).append("\n");
        builder.append("Date: ").append(order.getOrderTime()).append("\n");
        builder.append("Customer: ").append(order.getCustomerName()).append(" (").append(order.getUserEmail()).append(")\n");
        builder.append("Status: ").append(order.getStatus()).append("\n\n");
        builder.append("Items:\n");
        if (order.getItems() != null) {
            for (CartItem item : order.getItems()) {
                builder.append("- ")
                        .append(item.getName())
                        .append(" x ")
                        .append(item.getQuantity())
                        .append(" = Rs ")
                        .append(Math.round(item.getTotalPrice()))
                        .append("\n");
            }
        }
        builder.append("\nSubtotal: Rs ").append(Math.round(order.getSubtotalAmount() > 0 ? order.getSubtotalAmount() : order.getTotalAmount() + order.getDiscountAmount())).append("\n");
        builder.append("Discount: Rs ").append(Math.round(order.getDiscountAmount())).append("\n");
        builder.append("Payable: Rs ").append(Math.round(order.getTotalAmount())).append("\n");
        builder.append("Payment: ").append(order.getPaymentMode()).append(" / ").append(order.getPaymentStatus()).append("\n");
        return builder.toString();
    }

    private Map<String, Double> revenueByDay(List<Order> orders) {
        Map<String, Double> revenue = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            revenue.put(day.format(formatter), 0.0);
        }

        for (Order order : orders) {
            if (order.getOrderTime() == null || order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            LocalDate day = order.getOrderTime().toLocalDate();
            String key = day.format(formatter);
            if (revenue.containsKey(key)) {
                revenue.put(key, revenue.get(key) + order.getTotalAmount());
            }
        }

        return revenue;
    }

    private Map<String, Double> categoryRevenue(List<Order> orders) {
        Map<String, Double> revenue = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getItems() == null) {
                continue;
            }

            for (CartItem item : order.getItems()) {
                String category = item.getCategory() == null || item.getCategory().isBlank()
                        ? "Meals"
                        : item.getCategory();
                revenue.put(category, revenue.getOrDefault(category, 0.0) + item.getTotalPrice());
            }
        }

        return revenue;
    }

    private List<Map<String, Object>> topProducts(List<Order> orders) {
        Map<String, Integer> quantityByName = new LinkedHashMap<>();
        Map<String, Double> revenueByName = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getItems() == null || order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }

            for (CartItem item : order.getItems()) {
                quantityByName.put(item.getName(), quantityByName.getOrDefault(item.getName(), 0) + item.getQuantity());
                revenueByName.put(item.getName(), revenueByName.getOrDefault(item.getName(), 0.0) + item.getTotalPrice());
            }
        }

        return quantityByName.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> Map.<String, Object>of(
                        "name", entry.getKey(),
                        "quantity", entry.getValue(),
                        "revenue", revenueByName.getOrDefault(entry.getKey(), 0.0)
                ))
                .collect(Collectors.toList());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
