
/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    Double servingRadiusInKms;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    if (((currentTime.isAfter(LocalTime.parse("07:59:59", formatter)))
        && (currentTime.isBefore(LocalTime.parse("10:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("12:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("14:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("18:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("21:00:01", formatter))))) {
      servingRadiusInKms = peakHoursServingRadiusInKms;
    } else {
      servingRadiusInKms = normalHoursServingRadiusInKms;
    }
    List<Restaurant> restaurants =
        restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(),
            getRestaurantsRequest.getLongitude(), currentTime, servingRadiusInKms);
    return new GetRestaurantsResponse(restaurants);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    Double servingRadiusInKms;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    if (((currentTime.isAfter(LocalTime.parse("07:59:59", formatter)))
        && (currentTime.isBefore(LocalTime.parse("10:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("12:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("14:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("18:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("21:00:01", formatter))))) {
      servingRadiusInKms = peakHoursServingRadiusInKms;
    } else {
      servingRadiusInKms = normalHoursServingRadiusInKms;
    }

    List<Restaurant> restaurants = new ArrayList<>();
    List<List<Restaurant>> allRestaurants = new ArrayList<>();

    if (getRestaurantsRequest.getSearchFor().length() != 0) {

      allRestaurants.add(new ArrayList<>(restaurantRepositoryService.findRestaurantsByName(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
          getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms)));

      allRestaurants.add(new ArrayList<>(restaurantRepositoryService.findRestaurantsByAttributes(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
          getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms)));

      allRestaurants.add(new ArrayList<>(restaurantRepositoryService.findRestaurantsByItemName(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
          getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms)));

      allRestaurants
          .add(new ArrayList<>(restaurantRepositoryService.findRestaurantsByItemAttributes(
              getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
              getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms)));

      // restaurants = allRestaurants.stream()
      // .flatMap(List::stream)
      // .collect(Collectors.toList());

      for (List<Restaurant> restaurant : allRestaurants) {
        for (Restaurant restaurant2 : restaurant) {
          restaurants.add(restaurant2);
        }
      }

      return new GetRestaurantsResponse(restaurants);

    }
    return new GetRestaurantsResponse(restaurants);
  }


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double servingRadiusInKms;
    List<Restaurant> restaurants = new ArrayList<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    if (((currentTime.isAfter(LocalTime.parse("07:59:59", formatter)))
        && (currentTime.isBefore(LocalTime.parse("10:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("12:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("14:00:01", formatter))))
        || ((currentTime.isAfter(LocalTime.parse("18:59:59", formatter)))
            && (currentTime.isBefore(LocalTime.parse("21:00:01", formatter))))) {
      servingRadiusInKms = peakHoursServingRadiusInKms;
    } else {
      servingRadiusInKms = normalHoursServingRadiusInKms;
    }
    getRestaurantsRequest.getSearchFor();
    Set<String> restaurantSet = new HashSet<>();
    List<List<Restaurant>> allRestaurants = new ArrayList<>();

    if (getRestaurantsRequest.getSearchFor().length() != 0) {


      // fetching returant data byResturnatName
      Future<List<Restaurant>> futureGetRestaurantsByNameList =
          restaurantRepositoryService.findRestaurantsByNameAsync(
              getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
              getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms);

      List<Restaurant> restaurantByName = getDataFromThreadPool(futureGetRestaurantsByNameList);

      // fetching data By Resturnat Attributes
      Future<List<Restaurant>> futureGetRestaurantsByResturantAttributes =
          restaurantRepositoryService.findRestaurantsByAttributesAsync(
              getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
              getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms);
      List<Restaurant> restaurantByResturantAttributes =
          getDataFromThreadPool(futureGetRestaurantsByResturantAttributes);


      // fetching data from item name
      Future<List<Restaurant>> futureGetRestaurantsByItemName =
          restaurantRepositoryService.findRestaurantsByItemNameAsync(
              getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
              getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms);

      List<Restaurant> restaurantsByItemName =
          getDataFromThreadPool(futureGetRestaurantsByItemName);

      // fetching from itemAttributes
      Future<List<Restaurant>> futureGetRestaurantsByItemAttributes =
          restaurantRepositoryService.findRestaurantsByItemAttributesAsync(
              getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
              getRestaurantsRequest.getSearchFor(), currentTime, servingRadiusInKms);

      List<Restaurant> restaurantsByItemAttributes =
          getDataFromThreadPool(futureGetRestaurantsByItemAttributes);

      allRestaurants.add(restaurantByName);
      allRestaurants.add(restaurantByResturantAttributes);
      allRestaurants.add(restaurantsByItemName);
      allRestaurants.add(restaurantsByItemAttributes);

      for (List<Restaurant> restaurant : allRestaurants) {
        if (restaurant != null)
          for (Restaurant restaurant2 : restaurant)
            if (!restaurantSet.contains(restaurant2.getRestaurantId())) {
              restaurantSet.add(restaurant2.getName());
              restaurants.add(restaurant2);
            }
      }
      return new GetRestaurantsResponse(restaurants);
    }
    return new GetRestaurantsResponse(restaurants);
  }

  /// Fetching data from Thread Pool
  public List<Restaurant> getDataFromThreadPool(Future<List<Restaurant>> future) {
    List<Restaurant> restaurant;
    try {
      while (true) {
        if (future.isDone()) {
          restaurant = future.get();
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
    return new ArrayList<>();
  }
}

