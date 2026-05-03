package com.foody.dto;

import com.foody.model.Address;
import com.foody.model.User;

import java.util.List;

public class UserProfileResponse {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean blocked;
    private List<Address> addresses;
    private List<String> wishlistProductIds;

    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.blocked = user.isBlocked();
        this.addresses = user.getAddresses();
        this.wishlistProductIds = user.getWishlistProductIds();
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

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<String> getWishlistProductIds() {
        return wishlistProductIds;
    }
}
