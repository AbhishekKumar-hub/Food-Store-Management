package com.foody.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    public Map<String, Object> createPaymentOrder(double amount) {
        ensureConfigured();

        try {
            int amountInPaise = (int) Math.round(amount * 100);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "foodify_" + UUID.randomUUID().toString().substring(0, 8));
            orderRequest.put("payment_capture", 1);

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);

            return Map.of(
                    "enabled", true,
                    "keyId", keyId,
                    "orderId", razorpayOrder.get("id"),
                    "amount", razorpayOrder.get("amount"),
                    "currency", razorpayOrder.get("currency")
            );
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create Razorpay order: " + ex.getMessage());
        }
    }

    public Map<String, Object> getConfig() {
        return Map.of(
                "enabled", isConfigured(),
                "keyId", keyId == null ? "" : keyId
        );
    }

    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        ensureConfigured();

        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(razorpaySignature)) {
            return false;
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (Exception ex) {
            return false;
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new RuntimeException("Razorpay keys are not configured yet");
        }
    }

    private boolean isConfigured() {
        return !isBlank(keyId) && !isBlank(keySecret);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
