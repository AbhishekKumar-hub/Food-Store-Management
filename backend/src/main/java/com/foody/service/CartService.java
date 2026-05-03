package com.foody.service;

import com.foody.model.Cart;
import com.foody.model.CartItem;
import com.foody.model.Product;
import com.foody.repository.CartRepository;
import com.foody.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Cart addToCart(String email, String productId, int quantity) {

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isAvailable() || product.getStock() <= 0) {
            throw new RuntimeException(product.getName() + " is out of stock");
        }

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserEmail(email);
                    c.setItems(new ArrayList<>());
                    return c;
                });

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        int alreadyInCart = (item == null) ? 0 : item.getQuantity();

        if (alreadyInCart + quantity > product.getStock()) {
            throw new RuntimeException(
                    "Only " + product.getStock() + " item(s) available for " + product.getName()
            );
        }

        if (item == null) {
            CartItem newItem = new CartItem();
            newItem.setProductId(productId);
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setCategory(product.getCategory());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        } else {
            item.setQuantity(alreadyInCart + quantity);
            item.setName(product.getName());
            item.setImageUrl(product.getImageUrl());
            item.setCategory(product.getCategory());
            item.setPrice(product.getPrice());
        }

        return cartRepository.save(cart);
    }

    public Cart getCart(String email) {
        return cartRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserEmail(email);
                    cart.setItems(new ArrayList<>());
                    return cartRepository.save(cart);
                });
    }

    public Cart removeItem(String email, String productId) {

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Iterator<CartItem> iterator = cart.getItems().iterator();
        boolean removed = false;

        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getProductId().equals(productId)) {
                iterator.remove();
                removed = true;
                break;
            }
        }

        if (!removed) {
            throw new RuntimeException("Item not found in cart");
        }

        if (cart.getItems().isEmpty()) {
            cartRepository.delete(cart);
            return new Cart();
        }

        return cartRepository.save(cart);
    }

    public Cart updateQuantity(String email, String productId, int quantity) {
        if (quantity <= 0) {
            return removeItem(email, productId);
        }

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isAvailable() || product.getStock() <= 0) {
            throw new RuntimeException(product.getName() + " is out of stock");
        }

        if (quantity > product.getStock()) {
            throw new RuntimeException(
                    "Only " + product.getStock() + " item(s) available for " + product.getName()
            );
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        item.setQuantity(quantity);
        item.setName(product.getName());
        item.setImageUrl(product.getImageUrl());
        item.setCategory(product.getCategory());
        item.setPrice(product.getPrice());

        return cartRepository.save(cart);
    }

    public void clearCart(String email) {
        cartRepository.findByUserEmail(email)
                .ifPresent(cartRepository::delete);
    }
}
