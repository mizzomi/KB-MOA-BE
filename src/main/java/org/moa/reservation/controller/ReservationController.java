package org.moa.reservation.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	 * 예약 내역 조회 API (페이지네이션 지원)
	 * @param tripId 여행 ID (필수)
	 * @param resKind 예약 종류 (선택) - TRANSPORT, ACCOMMODATION, RESTAURANT
	 * @param pageable 페이지네이션 정보 (page, size)
	 * @return 예약 내역 페이지
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<Page<ReservationItemResponseDto>>> getReservations(
		@RequestParam Long tripId,
		@RequestParam(required = false) String resKind,
		@PageableDefault(sort = "date") Pageable pageable) {

		log.info("예약 내역 조회 API 호출 - tripId: {}, resKind: {}, page: {}, size: {}",
			tripId, resKind, pageable.getPageNumber(), pageable.getPageSize());

		try {
			Page<ReservationItemResponseDto> reservations = reservationService.getReservations(tripId, resKind,
				pageable);

			log.info("예약 내역 조회 API 성공 - 총 {}건, 현재 페이지: {}/{}",
				reservations.getTotalElements(),
				reservations.getNumber() + 1,
				reservations.getTotalPages());

			return ResponseEntity.status(StatusCode.OK.getStatus())
				.body(ApiResponse.of(reservations));

		} catch (IllegalArgumentException e) {
			log.warn("잘못된 요청 파라미터 - tripId: {}, resKind: {}, error: {}", tripId, resKind, e.getMessage());
			return ResponseEntity.status(StatusCode.BAD_REQUEST.getStatus())
				.body(ApiResponse.error(StatusCode.BAD_REQUEST, e.getMessage()));

		} catch (Exception e) {
			log.error("예약 내역 조회 API 오류 - tripId: {}, resKind: {}", tripId, resKind, e);
			return ResponseEntity.status(StatusCode.INTERNAL_ERROR.getStatus())
				.body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "예약 내역 조회 중 오류가 발생했습니다."));
		}
	}

	/**
	 * 날짜별 예약 내역 조회 API (페이지네이션 지원)
	 * @param customUser 인증된 사용자 정보
	 * @param tripId 여행 ID (필수)
	 * @param date 조회할 날짜 (yyyy-MM-dd 형식, 필수)
	 * @param pageable 페이지네이션 정보 (page, size)
	 * @return 해당 날짜의 예약 내역 페이지 (예매시간 순 정렬)
	 */
	@GetMapping("/by-date")
	public ResponseEntity<ApiResponse<Page<ReservationItemResponseDto>>> getReservationsByDate(
		@AuthenticationPrincipal CustomUser customUser,
		@RequestParam Long tripId,
		@RequestParam String date,
		@PageableDefault(sort = "createdAt") Pageable pageable) {

		log.info("날짜별 예약 내역 조회 API 호출 - memberId: {}, tripId: {}, date: {}, page: {}, size: {}",
			customUser.getMember().getMemberId(), tripId, date, pageable.getPageNumber(), pageable.getPageSize());

		try {
			// 날짜 형식 검증
			LocalDate reservationDate;
			try {
				reservationDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException e) {
				log.warn("잘못된 날짜 형식 - date: {}", date);
				return ResponseEntity.status(StatusCode.BAD_REQUEST.getStatus())
					.body(ApiResponse.error(StatusCode.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd 형식으로 입력해주세요)"));
			}

			Long memberId = customUser.getMember().getMemberId();

			Page<ReservationItemResponseDto> reservations = reservationService.getReservationsByDate(
				memberId, tripId, reservationDate, pageable);

			log.info("날짜별 예약 내역 조회 API 성공 - 총 {}건, 현재 페이지: {}/{}",
				reservations.getTotalElements(),
				reservations.getNumber() + 1,
				reservations.getTotalPages());

			return ResponseEntity.status(StatusCode.OK.getStatus())
				.body(ApiResponse.of(reservations));

		} catch (IllegalArgumentException e) {
			log.warn("잘못된 요청 파라미터 - tripId: {}, date: {}, error: {}", tripId, date, e.getMessage());
			return ResponseEntity.status(StatusCode.BAD_REQUEST.getStatus())
				.body(ApiResponse.error(StatusCode.BAD_REQUEST, e.getMessage()));

		} catch (Exception e) {
			log.error("날짜별 예약 내역 조회 API 오류 - tripId: {}, date: {}", tripId, date, e);
			return ResponseEntity.status(StatusCode.INTERNAL_ERROR.getStatus())
				.body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "날짜별 예약 내역 조회 중 오류가 발생했습니다."));
		}
	}
}
