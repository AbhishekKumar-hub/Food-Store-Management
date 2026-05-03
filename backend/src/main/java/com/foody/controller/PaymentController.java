package com.foody.controller;

import com.foody.dto.PaymentOrderRequest;
import com.foody.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return paymentService.getConfig();
    }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@Valid @RequestBody PaymentOrderRequest request) {
        return paymentService.createPaymentOrder(request.getAmount());
    }
}
