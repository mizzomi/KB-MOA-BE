package org.moa.reservation.restaurant.service;

import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.restaurant.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RestaurantService {
    // 예약 가능한 식당 조회
    Page<RestaurantListResponseDto> getAvailableRestaurants(Long tripId, LocalDate date, String category, Pageable pageable);

    // 식당 정보 조회
    RestaurantInfoResponseDto getRestaurantInfo(Long restId);

    // 예약 가능한 시간대 조회
    List<AvailableTimeResponseDto> getAvailableTime(Long restId, LocalDate date);

    // 식당 예약 생성
    void createReservation(Long memberId, RestaurantReservationRequestDto dto);

    // 여행별 식당 예약 조회
    List<RestaurantReservationResponseDto> getReservations(Long tripId);

    // 식당 예약 상세 조회
    RestaurantReservationDetailDto getReservationDetail(Long restResId);

    //tripId로 식당 예약 리스트 조회
    List<ReservationItemResponseDto> getRestaurantReservations(Long tripId);
}
