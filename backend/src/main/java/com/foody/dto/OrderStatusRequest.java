package com.foody.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    public OrderStatusRequest() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
