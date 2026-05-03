package com.foody.service;

import com.foody.dto.AddressRequest;
import com.foody.dto.AdminUserResponse;
import com.foody.dto.ProfileUpdateRequest;
import com.foody.dto.RegisterRequest;
import com.foody.dto.UserProfileResponse;
import com.foody.model.Address;
import com.foody.model.Order;
import com.foody.model.User;
import com.foody.repository.OrderRepository;
import com.foody.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
    }

    public String registerUser(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

       
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setPhone(request.getPhone());


        user.setRole("ROLE_CUSTOMER");
        user.setAddresses(new ArrayList<>());
        user.setWishlistProductIds(new ArrayList<>());

        userRepository.save(user);

        return "User registered successfully";
    }

    public UserProfileResponse getProfile(String email) {
        return new UserProfileResponse(getUserByEmail(email));
    }

    public UserProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = getUserByEmail(email);
        user.setName(request.getName().trim());
        user.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        return new UserProfileResponse(userRepository.save(user));
    }

    public UserProfileResponse addAddress(String email, AddressRequest request) {
        User user = getUserByEmail(email);
        List<Address> addresses = ensureAddresses(user);
        Address address = toAddress(request);
        address.setId(UUID.randomUUID().toString());

        if (addresses.isEmpty() || request.isDefaultAddress()) {
            addresses.forEach(a -> a.setDefaultAddress(false));
            address.setDefaultAddress(true);
        }

        addresses.add(address);
        user.setAddresses(addresses);
        return new UserProfileResponse(userRepository.save(user));
    }

    public UserProfileResponse updateAddress(String email, String addressId, AddressRequest request) {
        User user = getUserByEmail(email);
        List<Address> addresses = ensureAddresses(user);
        Address address = findAddress(addresses, addressId);

        address.setLabel(cleanOrDefault(request.getLabel(), "Home"));
        address.setLine1(request.getLine1().trim());
        address.setLandmark(cleanOrDefault(request.getLandmark(), ""));
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPincode(request.getPincode().trim());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        if (request.isDefaultAddress()) {
            addresses.forEach(a -> a.setDefaultAddress(false));
            address.setDefaultAddress(true);
        }

        user.setAddresses(addresses);
        return new UserProfileResponse(userRepository.save(user));
    }

    public UserProfileResponse deleteAddress(String email, String addressId) {
        User user = getUserByEmail(email);
        List<Address> addresses = ensureAddresses(user);
        Address existing = findAddress(addresses, addressId);
        boolean wasDefault = existing.isDefaultAddress();

        addresses.removeIf(address -> address.getId().equals(addressId));
        if (wasDefault && !addresses.isEmpty()) {
            addresses.get(0).setDefaultAddress(true);
        }

        user.setAddresses(addresses);
        return new UserProfileResponse(userRepository.save(user));
    }

    public UserProfileResponse setDefaultAddress(String email, String addressId) {
        User user = getUserByEmail(email);
        List<Address> addresses = ensureAddresses(user);
        Address selected = findAddress(addresses, addressId);
        addresses.forEach(address -> address.setDefaultAddress(false));
        selected.setDefaultAddress(true);
        user.setAddresses(addresses);
        return new UserProfileResponse(userRepository.save(user));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Address findDeliveryAddress(String email, String addressId) {
        User user = getUserByEmail(email);
        List<Address> addresses = ensureAddresses(user);

        if (addressId != null && !addressId.isBlank()) {
            return copyAddress(findAddress(addresses, addressId));
        }

        return addresses.stream()
                .filter(Address::isDefaultAddress)
                .findFirst()
                .map(this::copyAddress)
                .orElseThrow(() -> new RuntimeException("Please add a delivery address before placing the order"));
    }

    public List<String> getWishlist(String email) {
        User user = getUserByEmail(email);
        return ensureWishlist(user);
    }

    public List<String> addToWishlist(String email, String productId) {
        User user = getUserByEmail(email);
        List<String> wishlist = ensureWishlist(user);
        if (!wishlist.contains(productId)) {
            wishlist.add(productId);
        }
        user.setWishlistProductIds(wishlist);
        return userRepository.save(user).getWishlistProductIds();
    }

    public List<String> removeFromWishlist(String email, String productId) {
        User user = getUserByEmail(email);
        List<String> wishlist = ensureWishlist(user);
        wishlist.remove(productId);
        user.setWishlistProductIds(wishlist);
        return userRepository.save(user).getWishlistProductIds();
    }

    public List<AdminUserResponse> getUsersForAdmin() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(User::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(user -> {
                    List<Order> orders = orderRepository.findByUserEmail(user.getEmail());
                    double totalSpent = orders.stream()
                            .filter(order -> order.getStatus() != com.foody.model.OrderStatus.CANCELLED)
                            .mapToDouble(Order::getTotalAmount)
                            .sum();
                    return new AdminUserResponse(user, orders.size(), totalSpent);
                })
                .toList();
    }

    public AdminUserResponse setBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Admin users cannot be blocked from the dashboard");
        }

        user.setBlocked(blocked);
        User saved = userRepository.save(user);
        List<Order> orders = orderRepository.findByUserEmail(saved.getEmail());
        double totalSpent = orders.stream()
                .filter(order -> order.getStatus() != com.foody.model.OrderStatus.CANCELLED)
                .mapToDouble(Order::getTotalAmount)
                .sum();
        return new AdminUserResponse(saved, orders.size(), totalSpent);
    }

    private List<Address> ensureAddresses(User user) {
        if (user.getAddresses() == null) {
            user.setAddresses(new ArrayList<>());
        }
        return user.getAddresses();
    }

    private List<String> ensureWishlist(User user) {
        if (user.getWishlistProductIds() == null) {
            user.setWishlistProductIds(new ArrayList<>());
        }
        return user.getWishlistProductIds();
    }

    private Address findAddress(List<Address> addresses, String addressId) {
        return addresses.stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    private Address toAddress(AddressRequest request) {
        Address address = new Address();
        address.setLabel(cleanOrDefault(request.getLabel(), "Home"));
        address.setLine1(request.getLine1().trim());
        address.setLandmark(cleanOrDefault(request.getLandmark(), ""));
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPincode(request.getPincode().trim());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefaultAddress(request.isDefaultAddress());
        return address;
    }

    private Address copyAddress(Address source) {
        Address copy = new Address();
        copy.setId(source.getId());
        copy.setLabel(source.getLabel());
        copy.setLine1(source.getLine1());
        copy.setLandmark(source.getLandmark());
        copy.setCity(source.getCity());
        copy.setState(source.getState());
        copy.setPincode(source.getPincode());
        copy.setLatitude(source.getLatitude());
        copy.setLongitude(source.getLongitude());
        copy.setDefaultAddress(source.isDefaultAddress());
        return copy;
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
