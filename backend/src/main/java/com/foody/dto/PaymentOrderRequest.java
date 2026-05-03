package com.foody.dto;

import jakarta.validation.constraints.Positive;

public class PaymentOrderRequest {

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    public PaymentOrderRequest() {}

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
