package org.moa.reservation.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.reservation.restaurant.dto.AvailableTimeResponseDto;
import org.moa.reservation.restaurant.dto.RestaurantListResponseDto;
import org.moa.reservation.restaurant.mapper.RestaurantMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantMapper restaurantMapper;

    // 예약 가능한 식당 조회
    @Override
    public Page<RestaurantListResponseDto> getAvailableRestaurants(Long tripId, LocalDate date, String category, Pageable pageable) {

        List<RestaurantListResponseDto> restaurants =
                restaurantMapper.findAvailableRestaurant(tripId, date, category, pageable);

        int total = restaurantMapper.countAvailableRestaurants(tripId, date, category);

        return new PageImpl<>(restaurants, pageable, total);

    }

    // 예약 가능한 시간대 조회
    @Override
    public List<AvailableTimeResponseDto> getAvailableTime(Long restId, LocalDate date) {

        List<String> timeSlots = restaurantMapper.findTimeSlot(restId);

        List<AvailableTimeResponseDto> availableTimes = new ArrayList<>();

        for(String timeSlot : timeSlots) {
            int max = restaurantMapper.findMaxCapacity(restId, timeSlot);
            int reserved = restaurantMapper.findReservedCount(restId, date, timeSlot);
            int available = max - reserved;
            
            // 음수 방지
            available = Math.max(available, 0);

            if (available > 0) {
                availableTimes.add(new AvailableTimeResponseDto(timeSlot, max, reserved, available));
            }
        }

        return availableTimes;
    }

}
