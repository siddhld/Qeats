/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.models;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Java class that maps to Mongo collection.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "restaurants")
public class RestaurantEntity {

  @Id
  @JsonIgnore
  private String id;

  @NotNull
  private String restaurantId;

  @NotNull
  private String name;

  @NotNull
  private String city;

  @NotNull
  private String imageUrl;

  @NotNull
  private Double latitude;

  @NotNull
  private Double longitude;

  @NotNull
  private String opensAt;

  @NotNull
  private String closesAt;

  @NotNull
  private List<String> attributes = new ArrayList<>();

  public String getId() {
    return id;
  }

  public String getRestaurantId() {
    return restaurantId;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getOpensAt() {
    return opensAt;
  }

  public String getClosesAt() {
    return closesAt;
  }

  public List<String> getAttributes() {
    return attributes;
  }

}

