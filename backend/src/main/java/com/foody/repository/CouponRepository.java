package com.foody.repository;

import com.foody.model.Coupon;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CouponRepository extends MongoRepository<Coupon, String> {

    Optional<Coupon> findByCodeIgnoreCase(String code);
}
