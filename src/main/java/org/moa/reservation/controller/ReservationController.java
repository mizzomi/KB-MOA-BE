package org.moa.reservation.controller;

import java.util.ArrayList;
import java.util.List;

import org.moa.global.response.ApiResponse;
import org.moa.global.type.StatusCode;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.accommodation.service.AccommodationService;
import org.moa.reservation.restaurant.service.RestaurantService;
import org.moa.reservation.service.ReservationService;
import org.moa.reservation.transport.service.TransportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	/**
	 * 예약 내역 조회 API
	 * @param tripId 여행 ID (필수)
	 * @param resKind 예약 종류 (선택) - TRANSPORT, ACCOMMODATION, RESTAURANT
	 * @return 예약 내역 리스트
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<ReservationItemResponseDto>>> getReservations(
		@RequestParam Long tripId,
		@RequestParam(required = false) String resKind) {

		log.info("예약 내역 조회 API 호출 - tripId: {}, resKind: {}", tripId, resKind);

		try {
			List<ReservationItemResponseDto> reservations = reservationService.getReservations(tripId, resKind);

			log.info("예약 내역 조회 API 성공 - 결과 건수: {}", reservations.size());
			return ResponseEntity.status(StatusCode.OK.getStatus())
				.body(ApiResponse.of(reservations));

		} catch (IllegalArgumentException e) {
			log.warn("잘못된 요청 파라미터 - tripId: {}, resKind: {}, error: {}", tripId, resKind, e.getMessage());
			return ResponseEntity.status(StatusCode.BAD_REQUEST.getStatus())
				.body(ApiResponse.error(StatusCode.BAD_REQUEST,e.getMessage()));

		} catch (Exception e) {
			log.error("예약 내역 조회 API 오류 - tripId: {}, resKind: {}", tripId, resKind, e);
			return ResponseEntity.status(StatusCode.INTERNAL_ERROR.getStatus())
				.body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "예약 내역 조회 중 오류가 발생했습니다."));
		}
	}
}
