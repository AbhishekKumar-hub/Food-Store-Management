package com.foody.service;

import com.foody.model.RestaurantInfo;
import com.foody.repository.RestaurantInfoRepository;
import org.springframework.stereotype.Service;

@Service
public class RestaurantInfoService {

    private final RestaurantInfoRepository restaurantInfoRepository;

    public RestaurantInfoService(RestaurantInfoRepository restaurantInfoRepository) {
        this.restaurantInfoRepository = restaurantInfoRepository;
    }

    public RestaurantInfo getRestaurant() {
        return restaurantInfoRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> restaurantInfoRepository.save(new RestaurantInfo()));
    }

    public RestaurantInfo updateRestaurant(RestaurantInfo updated) {
        RestaurantInfo restaurant = getRestaurant();
        restaurant.setName(cleanOrDefault(updated.getName(), "Foody Kitchen"));
        restaurant.setDescription(cleanOrDefault(updated.getDescription(), "Fresh meals prepared close to you."));
        restaurant.setPhone(cleanOrDefault(updated.getPhone(), ""));
        restaurant.setAddressLine(cleanOrDefault(updated.getAddressLine(), ""));
        restaurant.setCity(cleanOrDefault(updated.getCity(), ""));
        restaurant.setOpeningHours(cleanOrDefault(updated.getOpeningHours(), ""));
        restaurant.setLatitude(updated.getLatitude());
        restaurant.setLongitude(updated.getLongitude());
        return restaurantInfoRepository.save(restaurant);
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
