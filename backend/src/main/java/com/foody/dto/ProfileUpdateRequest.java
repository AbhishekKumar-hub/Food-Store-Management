package com.foody.dto;

import jakarta.validation.constraints.NotBlank;

public class ProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;

    public ProfileUpdateRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
