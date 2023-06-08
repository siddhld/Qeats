/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import java.time.LocalTime;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
// Implement Controller using Spring annotations.
// Remember, annotations have various "targets". They can be class level, method level or others.
@RestController

public class RestaurantController {

  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;

  @GetMapping(RESTAURANT_API_ENDPOINT + RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
      @Valid GetRestaurantsRequest getRestaurantsRequest) {

    //log.info("getRestaurants called with {}", getRestaurantsRequest);
    GetRestaurantsResponse getRestaurantsResponse;
    String searchFor = getRestaurantsRequest.getSearchFor();

    if (searchFor != null && !searchFor.isEmpty()) {
      getRestaurantsResponse = restaurantService
             .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
    } else { 
         //CHECKSTYLE:OFF
      getRestaurantsResponse = restaurantService
             .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());      
         //CHECKSTYLE:ON
        }
      //CHECKSTYLE:OFF
      
      //log.info("getRestaurants returned {}", getRestaurantsResponse);
      //CHECKSTYLE:ON
      if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {
        getRestaurantsResponse.getRestaurants().forEach(restaurant -> {
          restaurant.setName(restaurant.getName().replace("é", "e"));
         });
 }
      

    return ResponseEntity.ok().body(getRestaurantsResponse);
    // TODO: CRIO_TASK_MODULE_MULTITHREADING
    // Improve the performance of this GetRestaurants API
    // and keep the functionality same.
    // Get the list of open restaurants near the specified latitude/longitude & matching searchFor.
    // API URI: /qeats/v1/restaurants?latitude=21.93&longitude=23.0&searchFor=tamil
    // Method: GET
    // Query Params: latitude, longitude, searchFor(optional)
    // Success Output:
    // 1). If searchFor param is present, return restaurants as a list matching the following
    // criteria
    // 1) open now
    // 2) is near the specified latitude and longitude
    // 3) searchFor matching(partially or fully):
    // - restaurant name
    // - or restaurant attribute
    // - or item name
    // - or item attribute (all matching is done ignoring case)
    //
    // 4) order the list by following the rules before returning
    // 1) Restaurant name
    // - exact matches first
    // - partial matches second
    // 2) Restaurant attributes
    // - partial and full matches in any order
    // 3) Item name
    // - exact matches first
    // - partial matches second
    // 4) Item attributes
    // - partial and full matches in any order
    // Eg: For example, when user searches for "Udupi", "Udupi Bhavan" restaurant should
    // come ahead of restaurants having "Udupi" in attribute.
    // 2). If searchFor param is absent,
    // 1) If there are restaurants near by return the list
    // 2) Else return empty list
    //
    // - For peak hours: 8AM-10AM, 1PM-2PM, 7PM-9PM
    // - service radius is 3KMs.
    // - All other times
    // - serving radius is 5KMs.
    // - If there are no restaurants, return empty list of restaurants.
    //
    //

  }

  
}
