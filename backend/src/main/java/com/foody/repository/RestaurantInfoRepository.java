package com.foody.repository;

import com.foody.model.RestaurantInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantInfoRepository extends MongoRepository<RestaurantInfo, String> {
}
