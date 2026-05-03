package com.foody.service;

import com.foody.model.CartItem;
import com.foody.model.Order;
import com.foody.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${foody.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${foody.mail.from:}")
    private String fromAddress;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderPlacedEmail(User user, Order order) {
        if (user == null || order == null) {
            return;
        }

        String items = order.getItems()
                .stream()
                .map(item -> item.getName() + " x " + item.getQuantity())
                .collect(Collectors.joining(", "));

        String body = "Hi " + user.getName() + ",\n\n"
                + "Your Foody order has been placed successfully.\n"
                + "Order ID: " + order.getId() + "\n"
                + "Items: " + items + "\n"
                + "Total: Rs " + order.getTotalAmount() + "\n"
                + "Status: " + order.getStatus() + "\n\n"
                + "We will keep you updated as your food is prepared.";

        send(user.getEmail(), "Foody order placed - " + order.getId(), body);
    }

    public void sendOrderDeliveredEmail(Order order) {
        if (order == null) {
            return;
        }

        String body = "Hi " + safe(order.getCustomerName()) + ",\n\n"
                + "Your Foody order " + order.getId() + " has been delivered.\n"
                + "Please open your order history and share feedback for the dishes you ordered.";

        send(order.getUserEmail(), "Foody order delivered - feedback requested", body);
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled || to == null || to.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            System.out.println("Email skipped: " + ex.getMessage());
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "there" : value;
    }
}
