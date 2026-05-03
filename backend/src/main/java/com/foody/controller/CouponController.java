package com.foody.controller;

import com.foody.dto.CouponRequest;
import com.foody.dto.CouponValidationResponse;
import com.foody.model.Coupon;
import com.foody.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/api/coupons/validate")
    public CouponValidationResponse validate(@RequestParam String code,
                                             @RequestParam double subtotal) {
        return couponService.validate(code, subtotal);
    }

    @GetMapping("/api/admin/coupons")
    public List<Coupon> allCoupons() {
        return couponService.getAll();
    }

    @PostMapping("/api/admin/coupons")
    public Coupon create(@Valid @RequestBody CouponRequest request) {
        return couponService.create(request);
    }

    @PutMapping("/api/admin/coupons/{id}")
    public Coupon update(@PathVariable String id,
                         @Valid @RequestBody CouponRequest request) {
        return couponService.update(id, request);
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public void delete(@PathVariable String id) {
        couponService.delete(id);
    }
}
