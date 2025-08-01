package org.moa.reservation.restaurant.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.restaurant.dto.RestaurantListResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RestaurantMapper {
    // 현재 페이지에 해당하는 목록 가져오기
    List<RestaurantListResponseDto> findAvailableRestaurant(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category,
            @Param("pageable") Pageable pageable
            );

    // 전체 식당 개수 세기 (페이지용)
    int countAvailableRestaurants(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category
    );
}