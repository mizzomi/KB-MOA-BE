package org.moa.reservation.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantListResponseDto {
    private Long restId;
    private String restName;
    private String restImageUrl;
    private String description;
}
