package org.moa.reservation.restaurant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.moa.reservation.restaurant.dto.*;
import org.moa.reservation.restaurant.service.RestaurantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/reservation/restaurant")
public class RestaurantController {

    private final RestaurantService restaurantService;

    // 예약 가능한 식당 목록 조회 API
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<RestaurantListResponseDto>>> getAvailableRestaurants(
            @PageableDefault(sort = "restId") Pageable pageable,
            @RequestParam("tripId") Long tripId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam("category") String category
    ) {
        log.info("RestaurantController ==== getAvailableRestaurants page={} sort={} date={} category={}",
                pageable.getPageNumber(), pageable.getSort(), date, category);

        Page<RestaurantListResponseDto> page =
                restaurantService.getAvailableRestaurants(tripId, date, category, pageable);
        return ResponseEntity.ok(ApiResponse.of(page));
    }

    // 예약 가능한 시간 목록 조회 API
    @GetMapping("{restId}/times")
    public ResponseEntity<ApiResponse<List<AvailableTimeResponseDto>>> getAvailableTime(
            @PathVariable Long restId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        log.info("RestaurantController ==== getAvailableTime restId={} date={}", restId, date);

        List<AvailableTimeResponseDto> times = restaurantService.getAvailableTime(restId, date);

        return ResponseEntity.ok(ApiResponse.of(times));
    }

    // 식당 예약 생성 API
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createReservation(
            @AuthenticationPrincipal CustomUser user, // 로그인 사용자
            @RequestBody RestaurantReservationRequestDto request
    ) {
        Long memberId = user.getMember().getMemberId();
        restaurantService.createReservation(memberId, request);
        return ResponseEntity.ok(ApiResponse.of("식당 예약이 완료되었습니다."));
    }

    // 여행별 식당 예약 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantReservationResponseDto>>> getReservations(
            @RequestParam Long tripId
    ) {
        List<RestaurantReservationResponseDto> reservations = restaurantService.getReservations(tripId);
        return ResponseEntity.ok(ApiResponse.of(reservations));
    }

    // 식당 예약 상세 조회 API
    @GetMapping("/{restResId}")
    public ResponseEntity<ApiResponse<RestaurantReservationDetailDto>> getReservationDetail(
            @PathVariable Long restResId
    ) {
        RestaurantReservationDetailDto detail = restaurantService.getReservationDetail(restResId);
        return ResponseEntity.ok(ApiResponse.of(detail));
    }

}