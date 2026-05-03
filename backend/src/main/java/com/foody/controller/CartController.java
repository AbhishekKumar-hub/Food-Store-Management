package com.foody.controller;

import com.foody.model.Cart;
import com.foody.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add/{productId}")
    public Cart addToCart(
            @PathVariable String productId,
            @RequestParam int quantity,
            Authentication auth
    ) {
        return cartService.addToCart(auth.getName(), productId, quantity);
    }

    @GetMapping
    public Cart viewCart(Authentication auth) {
        return cartService.getCart(auth.getName());
    }

    @DeleteMapping("/remove/{productId}")
    public Cart removeItem(
            @PathVariable String productId,
            Authentication auth
    ) {
        return cartService.removeItem(auth.getName(), productId);
    }

    @PutMapping("/item/{productId}")
    public Cart updateQuantity(
            @PathVariable String productId,
            @RequestParam int quantity,
            Authentication auth
    ) {
        return cartService.updateQuantity(auth.getName(), productId, quantity);
    }

    @DeleteMapping("/clear")
    public String clearCart(Authentication auth) {
        cartService.clearCart(auth.getName());
        return "Cart cleared successfully";
    }
}
