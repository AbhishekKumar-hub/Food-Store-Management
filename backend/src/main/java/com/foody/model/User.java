package com.foody.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;
    private String email;
    private String password;
    private String phone;
    private String role;
    private boolean blocked;
    private List<Address> addresses = new ArrayList<>();
    private List<String> wishlistProductIds = new ArrayList<>();

    public User() {}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses == null ? new ArrayList<>() : addresses;
    }

    public List<String> getWishlistProductIds() {
        return wishlistProductIds;
    }

    public void setWishlistProductIds(List<String> wishlistProductIds) {
        this.wishlistProductIds = wishlistProductIds == null ? new ArrayList<>() : wishlistProductIds;
    }
}
