package org.moa.reservation.restaurant.service;

import org.moa.reservation.restaurant.dto.RestaurantListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface RestaurantService {
    Page<RestaurantListResponseDto> getAvailableRestaurants(Long tripId, LocalDate date, String category, Pageable pageable);
}
