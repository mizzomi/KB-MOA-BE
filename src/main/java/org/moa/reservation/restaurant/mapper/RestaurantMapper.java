package org.moa.reservation.restaurant.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.restaurant.dto.RestaurantListResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RestaurantMapper {
    // 1. 예약 가능한 식당 목록
    List<RestaurantListResponseDto> findAvailableRestaurant(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category,
            @Param("pageable") Pageable pageable
            );

    // 2. 페이지네이션을 위한 총 개수
    int countAvailableRestaurants(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category
    );

    // 3. 예약 가능한 시간대 조회
    List<String> findTimeSlot(@Param("restId") Long restId);

    // 4. 특정 시간에 예약된 인원 수
    int findReservedCount(@Param("restId") Long restId,
                          @Param("date") LocalDate date,
                          @Param("time") String time);

    // 5. 특정 시간대의 최대 예약 가능 인원
    int findMaxCapacity(@Param("restId") Long restId,
                        @Param("time") String time);
}