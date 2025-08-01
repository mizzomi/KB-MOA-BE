package org.moa.reservation.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.reservation.restaurant.dto.RestaurantListResponseDto;
import org.moa.reservation.restaurant.mapper.RestaurantMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantMapper restaurantMapper;

    @Override
    public Page<RestaurantListResponseDto> getAvailableRestaurants(Long tripId, LocalDate date, String category, Pageable pageable) {

        List<RestaurantListResponseDto> restaurants =
                restaurantMapper.findAvailableRestaurant(tripId, date, category, pageable);

        int total = restaurantMapper.countAvailableRestaurants(tripId, date, category);

        return new PageImpl<>(restaurants, pageable, total);

    }
}
