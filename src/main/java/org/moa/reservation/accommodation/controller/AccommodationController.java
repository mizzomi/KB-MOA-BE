package org.moa.reservation.accommodation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.reservation.accommodation.dto.AccommodationDetailResponse;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.reservation.accommodation.dto.AccommodationReservationRequestDto;
import org.moa.reservation.accommodation.dto.AccommodationRoomsResponse;
import org.moa.reservation.accommodation.mapper.AccommodationMapper;
import org.moa.reservation.accommodation.service.AccommodationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/reservation")
@RequiredArgsConstructor
public class AccommodationController {
    private final AccommodationService accommodationService;

    // 입실 날짜와 퇴실 날짜에 따른, 예약 가능한 숙소들을 조회하는 API 입니다.
    @GetMapping("/accommodation")
    public ResponseEntity<ApiResponse<Page<AccommodationInfoResponse>>> searchAccommodations(
            @PageableDefault(size = 10, sort = "accomId") Pageable pageable,
            @RequestParam Long tripId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkinDay,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkoutDay
    ) {
        log.info("AccommodationController ==== searchAccommodation page={} sort={} from {} to {}",
                pageable.getPageNumber(), pageable.getSort(),
                checkinDay, checkoutDay);
        Page<AccommodationInfoResponse> page = accommodationService.searchAccommodations(tripId, checkinDay, checkoutDay, pageable);
        return ResponseEntity.ok(ApiResponse.of(page));
    }

    // 선택한 숙소의 ID를 기반으로 숙소 정보를 조회하는 API 입니다.
    @GetMapping("/accommodation-detail")
    public ResponseEntity<ApiResponse<AccommodationDetailResponse>> getAccommodation(@RequestParam Long accomId){
        log.info("AccommodationController ==== getAccommodation accomId={}", accomId);
        return ResponseEntity.ok(ApiResponse.of(accommodationService.getAccommodation(accomId)));
    }

    // 입실 날짜, 퇴실 날짜, 인원에 맞는 숙소의 예약 가능한 방들을 조회하는 API 입니다.
    @GetMapping("/accommodation-rooms")
    public ResponseEntity<ApiResponse<List<AccommodationRoomsResponse>>> getRooms(
            @RequestParam Long accomId,
            @RequestParam Long tripId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkinDay,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkoutDay,
            @RequestParam Integer guests
    ){
        List<AccommodationRoomsResponse> accommodationRoomsResponses = accommodationService.getRooms(accomId, tripId, checkinDay, checkoutDay, guests);
        return ResponseEntity.ok(ApiResponse.of(accommodationRoomsResponses));
    }

    @PostMapping("/accommodation")
    public ResponseEntity<ApiResponse<?>> reserveRoom(@RequestBody AccommodationReservationRequestDto dto){
        return ResponseEntity.ok(ApiResponse.of(accommodationService.reserveRoom(dto)));
    }
}
