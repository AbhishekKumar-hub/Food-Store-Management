package com.foody.dto;

import com.foody.model.User;

public class AdminUserResponse {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean blocked;
    private int orderCount;
    private double totalSpent;

    public AdminUserResponse(User user, int orderCount, double totalSpent) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.blocked = user.isBlocked();
        this.orderCount = orderCount;
        this.totalSpent = totalSpent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }
}
