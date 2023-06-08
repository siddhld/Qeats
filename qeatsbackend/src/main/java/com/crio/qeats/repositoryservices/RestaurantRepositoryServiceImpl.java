/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import ch.hsr.geohash.GeoHash;
import redis.clients.jedis.Jedis;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {


  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }


  public List<Restaurant> findAllRestaurantsMongo(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantEntityList = restaurantRepository.findAll();

    List<Restaurant> restaurantList = new ArrayList();
    for (RestaurantEntity restaurantEntity : restaurantEntityList) {

      if (isOpenNow(currentTime, restaurantEntity)) {
        if (GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
            restaurantEntity.getLongitude()) < servingRadiusInKms) {
          restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
        }
      }
    }

    String restaurantDbString = "";
    redisConfiguration.initCache();
    try {
      restaurantDbString = new ObjectMapper().writeValueAsString(restaurantList);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // System.out.print(restaurantDbString);

    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash =
        GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);
    Jedis jedis = redisConfiguration.getJedisPool().getResource();
    jedis.set(geoHash.toBase32(), restaurantDbString);

    return restaurantList;
  }


  private List<Restaurant> findAllRestaurantsCache(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurantList = new ArrayList<>();

    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash =
        GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);

    Jedis jedis = null;
    try {
      jedis = redisConfiguration.getJedisPool().getResource();

      String jsonStringFromCache = jedis.get(geoHash.toBase32());



      if (jsonStringFromCache == null) {
        // Cache needs to be updated.
        String createdJsonString = "";
        try {
          // restaurantList = findAllRestaurantsMongo(geoLocation.getLatitude(),
          // geoLocation.getLongitude(), currentTime, servingRadiusInKms);
          if (!jedis.exists(geoHash.toBase32())) {
            return findAllRestaurantsMongo(geoLocation.getLatitude(), geoLocation.getLongitude(),
                currentTime, servingRadiusInKms);
          }
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

        // Do operations with jedis resource
        // jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS,
        // createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache,
              new TypeReference<List<Restaurant>>() {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }

    return restaurantList;
  }


  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    if (redisConfiguration.isCacheAvailable()) {
      return findAllRestaurantsCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      return findAllRestaurantsMongo(latitude, longitude, currentTime, servingRadiusInKms);
    }

  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    Optional<List<RestaurantEntity>> resturantEntities =
        restaurantRepository.findRestaurantsByNameExact(searchString);
    Set<String> set = new HashSet<>();
    List<Restaurant> restaurantList = new ArrayList<>();
    ModelMapper mapper = modelMapperProvider.get();

    if (resturantEntities.isPresent()) {
      List<RestaurantEntity> entities = resturantEntities.get();
      for (RestaurantEntity entity : entities) {
        if (isRestaurantCloseByAndOpen(entity, currentTime, latitude, longitude, servingRadiusInKms)
            && !set.contains(entity.getId())) {
          restaurantList.add(mapper.map(entity, Restaurant.class));
          set.add(entity.getId());
        }
      }
    }
    return restaurantList;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> filteredRestaurants = new ArrayList<>();
    Optional<List<RestaurantEntity>> optionalRestaurantEntities =
        restaurantRepository.findRestaurantsByNameExact(searchString);
    if (optionalRestaurantEntities.isPresent()) {
      List<RestaurantEntity> restaurantEntities = optionalRestaurantEntities.get();
      Set<String> set = new HashSet<>();
      ModelMapper mapper = modelMapperProvider.get();

      for (RestaurantEntity entity : restaurantEntities) {
        if (isRestaurantCloseByAndOpen(entity, currentTime, latitude, longitude, servingRadiusInKms)
            && !set.contains(entity.getId())) {
          filteredRestaurants.add(mapper.map(entity, Restaurant.class));
          set.add(entity.getId());
         }
      }

    }

  return filteredRestaurants;

  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {


    List<Restaurant> restaurantList = new ArrayList<>();
    List<ItemEntity> itemEntities = itemRepository.findItemsByItemName(searchString);
    List<String> itemId = itemEntities.stream().map(ItemEntity::getId).collect(Collectors.toList());

    List<MenuEntity> menuEntities = menuRepository.findMenusByItemsItemIdIn(itemId).get();

    List<String> resturanId =
        menuEntities.stream().map(e -> e.getRestaurantId()).collect(Collectors.toList());

    List<RestaurantEntity> restaurantEntities = new ArrayList<>();

    for (String id : resturanId) {
      restaurantEntities.add(restaurantRepository.findById(id).get());
    }

    Set<String> set = new HashSet<>();
    ModelMapper mapper = modelMapperProvider.get();

    if (itemEntities.size() != 0) {
      for (RestaurantEntity entity : restaurantEntities) {
        if (isRestaurantCloseByAndOpen(entity, currentTime, latitude, longitude, servingRadiusInKms)
            && !set.contains(entity.getId())) {
          restaurantList.add(mapper.map(entity, Restaurant.class));
          set.add(entity.getId());
        }
      }
    }
    return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    ModelMapper modelMapper = modelMapperProvider.get();
    List<ItemEntity> matchingItems = itemRepository.findItemsByItemName(searchString);

    Set<String> itemIds = matchingItems.stream().map(ItemEntity::getId).collect(Collectors.toSet());

    List<MenuEntity> menusLists =
        menuRepository.findMenusByItemsItemIdIn(new ArrayList<>(itemIds)).get();

    Set<String> restaurantIds =
        menusLists.stream().map(MenuEntity::getRestaurantId).collect(Collectors.toSet());

    List<RestaurantEntity> restaurantEntities = new ArrayList<>();

    for (String id : restaurantIds) {
      restaurantEntities.add(restaurantRepository.findById(id).get());
    }
    
    List<Restaurant> restaurants = new ArrayList<>();
    restaurants   = restaurantEntities.stream()
        .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
            latitude, longitude, servingRadiusInKms))
        .map(restaurantEntity -> modelMapper.map(restaurantEntity, Restaurant.class))
        .collect(Collectors.toList());

    return restaurants;
  }


  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
          restaurantEntity.getLongitude()) < servingRadiusInKms;
    }
    return false;
  }


  //////////// Multithreading Operations

  @Override
  public Future<List<Restaurant>> findRestaurantsByNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    List<Restaurant> resturants =
        findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
    return new AsyncResult<>(resturants);

  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> resturants = findRestaurantsByAttributes(latitude, longitude, searchString,
        currentTime, servingRadiusInKms);

    return new AsyncResult<>(resturants);
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByItemNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> resturants = findRestaurantsByItemName(latitude, longitude, searchString,
        currentTime, servingRadiusInKms);

    return new AsyncResult<>(resturants);
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByItemAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> resturants = findRestaurantsByItemAttributes(latitude, longitude, searchString,
        currentTime, servingRadiusInKms);
    return new AsyncResult<>(resturants);
  }

}
