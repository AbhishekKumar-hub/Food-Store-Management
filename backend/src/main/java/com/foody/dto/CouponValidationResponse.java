package com.foody.dto;

public class CouponValidationResponse {

    private boolean valid;
    private String message;
    private String code;
    private double subtotal;
    private double discountAmount;
    private double payableAmount;

    public CouponValidationResponse() {}

    public CouponValidationResponse(boolean valid, String message, String code, double subtotal, double discountAmount) {
        this.valid = valid;
        this.message = message;
        this.code = code;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.payableAmount = Math.max(subtotal - discountAmount, 0);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getPayableAmount() {
        return payableAmount;
    }
}
