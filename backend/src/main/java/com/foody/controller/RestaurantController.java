package com.foody.controller;

import com.foody.model.RestaurantInfo;
import com.foody.service.RestaurantInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    private final RestaurantInfoService restaurantInfoService;

    public RestaurantController(RestaurantInfoService restaurantInfoService) {
        this.restaurantInfoService = restaurantInfoService;
    }

    @GetMapping
    public RestaurantInfo restaurant() {
        return restaurantInfoService.getRestaurant();
    }

    @PutMapping
    public RestaurantInfo update(@RequestBody RestaurantInfo restaurantInfo) {
        return restaurantInfoService.updateRestaurant(restaurantInfo);
    }
}
