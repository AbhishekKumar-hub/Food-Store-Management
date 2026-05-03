package com.foody.controller;

import com.foody.dto.AddressRequest;
import com.foody.dto.ProfileUpdateRequest;
import com.foody.dto.UserProfileResponse;
import com.foody.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication auth) {
        return userService.getProfile(auth.getName());
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request,
                                        Authentication auth) {
        return userService.updateProfile(auth.getName(), request);
    }

    @PostMapping("/me/addresses")
    public UserProfileResponse addAddress(@Valid @RequestBody AddressRequest request,
                                          Authentication auth) {
        return userService.addAddress(auth.getName(), request);
    }

    @PutMapping("/me/addresses/{addressId}")
    public UserProfileResponse updateAddress(@PathVariable String addressId,
                                             @Valid @RequestBody AddressRequest request,
                                             Authentication auth) {
        return userService.updateAddress(auth.getName(), addressId, request);
    }

    @PutMapping("/me/addresses/{addressId}/default")
    public UserProfileResponse makeDefault(@PathVariable String addressId,
                                           Authentication auth) {
        return userService.setDefaultAddress(auth.getName(), addressId);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public UserProfileResponse deleteAddress(@PathVariable String addressId,
                                             Authentication auth) {
        return userService.deleteAddress(auth.getName(), addressId);
    }

    @GetMapping("/me/wishlist")
    public List<String> wishlist(Authentication auth) {
        return userService.getWishlist(auth.getName());
    }

    @PostMapping("/me/wishlist/{productId}")
    public List<String> addWishlist(@PathVariable String productId,
                                    Authentication auth) {
        return userService.addToWishlist(auth.getName(), productId);
    }

    @DeleteMapping("/me/wishlist/{productId}")
    public List<String> removeWishlist(@PathVariable String productId,
                                       Authentication auth) {
        return userService.removeFromWishlist(auth.getName(), productId);
    }
}
