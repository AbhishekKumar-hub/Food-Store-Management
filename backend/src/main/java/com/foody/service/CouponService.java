package com.foody.service;

import com.foody.dto.CouponRequest;
import com.foody.dto.CouponValidationResponse;
import com.foody.model.Coupon;
import com.foody.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon create(CouponRequest request) {
        couponRepository.findByCodeIgnoreCase(request.getCode())
                .ifPresent(coupon -> {
                    throw new RuntimeException("Coupon code already exists");
                });
        return couponRepository.save(toCoupon(new Coupon(), request));
    }

    public Coupon update(String id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        couponRepository.findByCodeIgnoreCase(request.getCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Coupon code already exists");
                });
        return couponRepository.save(toCoupon(coupon, request));
    }

    public List<Coupon> getAll() {
        return couponRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Coupon::getCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public void delete(String id) {
        couponRepository.deleteById(id);
    }

    public CouponValidationResponse validate(String code, double subtotal) {
        if (code == null || code.isBlank()) {
            return new CouponValidationResponse(false, "Enter a coupon code", "", subtotal, 0);
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElse(null);

        if (coupon == null) {
            return new CouponValidationResponse(false, "Coupon not found", code.trim().toUpperCase(), subtotal, 0);
        }

        if (!coupon.isActive()) {
            return new CouponValidationResponse(false, "Coupon is inactive", coupon.getCode(), subtotal, 0);
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            return new CouponValidationResponse(false, "Coupon expired", coupon.getCode(), subtotal, 0);
        }

        if (subtotal < coupon.getMinOrderAmount()) {
            return new CouponValidationResponse(
                    false,
                    "Add items worth Rs " + Math.round(coupon.getMinOrderAmount() - subtotal) + " more",
                    coupon.getCode(),
                    subtotal,
                    0
            );
        }

        double discount = calculateDiscount(coupon, subtotal);
        return new CouponValidationResponse(true, "Coupon applied", coupon.getCode(), subtotal, discount);
    }

    private double calculateDiscount(Coupon coupon, double subtotal) {
        double discount;
        if ("FLAT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        } else {
            discount = subtotal * coupon.getDiscountValue() / 100;
        }

        if (coupon.getMaxDiscountAmount() > 0) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }

        return Math.min(discount, subtotal);
    }

    private Coupon toCoupon(Coupon coupon, CouponRequest request) {
        String code = request.getCode() == null ? "" : request.getCode().trim().toUpperCase();
        if (code.isBlank()) {
            throw new RuntimeException("Coupon code is required");
        }

        String discountType = request.getDiscountType() == null ? "PERCENT" : request.getDiscountType().trim().toUpperCase();
        if (!discountType.equals("PERCENT") && !discountType.equals("FLAT")) {
            throw new RuntimeException("Discount type must be PERCENT or FLAT");
        }

        if (request.getDiscountValue() <= 0) {
            throw new RuntimeException("Discount value must be greater than zero");
        }

        coupon.setCode(code);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(Math.max(request.getMinOrderAmount(), 0));
        coupon.setMaxDiscountAmount(Math.max(request.getMaxDiscountAmount(), 0));
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(request.isActive());
        return coupon;
    }
}
