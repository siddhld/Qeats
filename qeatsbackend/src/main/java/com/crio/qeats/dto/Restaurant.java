
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;


// @Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Restaurant {
      
      @JsonIgnore
      @NotNull
      private String id;
      private String restaurantId;
      private String name;
      private String  city;
      private String  imageUrl;
      private Double  latitude;
      private Double  longitude;
      private String opensAt;
      private String closesAt;
      private ArrayList<String> attributes ;

      public String getId() {
            return id;
      }

      public void setId(String id) {
            this.id = id;
      }

      public void setRestaurantId(String restaurantId) {
            this.restaurantId = restaurantId;
      }

      public String getName() {
            return name;
      }

      public void setName(String name) {
            this.name = name;
      }

      public String getCity() {
            return city;
      }

      public void setCity(String city) {
            this.city = city;
      }

      public String getImageUrl() {
            return imageUrl;
      }

      public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
      }

      public Double getLatitude() {
            return latitude;
      }

      public void setLatitude(Double latitude) {
            this.latitude = latitude;
      }

      public Double getLongitude() {
            return longitude;
      }

      public void setLongitude(Double longitude) {
            this.longitude = longitude;
      }

      public String getOpensAt() {
            return opensAt;
      }

      public void setOpensAt(String opensAt) {
            this.opensAt = opensAt;
      }

      public String getClosesAt() {
            return closesAt;
      }

      public void setClosesAt(String closesAt) {
            this.closesAt = closesAt;
      }

      public ArrayList<String> getAttributes() {
            return attributes;
      }

      public void setAttributes(ArrayList<String> attributes) {
            this.attributes = attributes;
      }

      public String getRestaurantId() {
            return restaurantId;
      }

      
}
