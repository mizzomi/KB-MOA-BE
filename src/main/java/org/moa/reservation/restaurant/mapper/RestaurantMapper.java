package org.moa.reservation.restaurant.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.restaurant.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RestaurantMapper {
    // 예약 가능한 식당 목록
    List<RestaurantListResponseDto> findAvailableRestaurant(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category,
            @Param("pageable") Pageable pageable
            );

    // 페이지네이션을 위한 총 개수
    int countAvailableRestaurants(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("category") String category
    );

    // 식당 정보 조회
    RestaurantInfoResponseDto findRestaurantInfo(Long restId);

    // 예약 가능한 시간대 조회
    List<String> findTimeSlot(@Param("restId") Long restId);

    // 특정 시간에 예약된 인원 수
    int findReservedCount(@Param("restTimeId") Long restTimeId);

    // 특정 시간대의 최대 예약 가능 인원
    int findMaxCapacity(@Param("restTimeId") Long restTimeId);

    // restTimeId 조회
    Long findRestTimeId(@Param("restId") Long restId, @Param("time") String time);

    // tripDayId 조회
    Long findTripDayId(@Param("tripId") Long tripId, @Param("date") LocalDate date);

    // reservationId 조회
    Long findLastInsertedReservationId();
    
    // 예약 등록
    int insertReservation(@Param("tripDayId") Long tripDayId);

    // 식당 예약 등록
    int insertRestaurantReservation(RestaurantReservationInsertDto insertDto);

    // 여행별 식당 예약 조회
    List<RestaurantReservationResponseDto> findReservations(@Param("tripId") Long tripId);

    // 식당 예약 상세 조회
    RestaurantReservationDetailDto findReservationDetail(@Param("restResId") Long restResId);

    List<ReservationItemResponseDto> getRestaurantReservationsByTripId(@Param("tripId") Long tripId);
    
    List<ReservationItemResponseDto> getRestaurantReservationsByDateAndMember(
        @Param("memberId") Long memberId, 
        @Param("tripId") Long tripId, 
        @Param("date") LocalDate date);
}
