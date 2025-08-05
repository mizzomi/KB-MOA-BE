package org.moa.reservation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.moa.reservation.accommodation.service.AccommodationService;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.restaurant.service.RestaurantService;
import org.moa.reservation.transport.service.TransportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

	private final TransportService transportService;
	private final AccommodationService accommodationService;
	private final RestaurantService restaurantService;

	@Override
	public Page<ReservationItemResponseDto> getReservations(Long tripId, String resKind, Pageable pageable) {
		log.info("예약 내역 조회 시작 - tripId: {}, resKind: {}, page: {}, size: {}",
			tripId, resKind, pageable.getPageNumber(), pageable.getPageSize());

		List<ReservationItemResponseDto> allReservations = new ArrayList<>();

		try {
			if (resKind == null || resKind.trim().isEmpty()) {
				// 전체 조회 - 모든 예약 종류 조회
				log.debug("전체 예약 내역 조회");
				allReservations.addAll(transportService.getTransportReservations(tripId));
				allReservations.addAll(accommodationService.getAccommodationReservations(tripId));
				allReservations.addAll(restaurantService.getRestaurantReservations(tripId));
			} else {
				// 특정 종류만 조회
				log.debug("특정 예약 종류 조회: {}", resKind);
				switch (resKind.toUpperCase().trim()) {
					case "TRANSPORT":
						allReservations.addAll(transportService.getTransportReservations(tripId));
						break;
					case "ACCOMMODATION":
						allReservations.addAll(accommodationService.getAccommodationReservations(tripId));
						break;
					case "RESTAURANT":
						allReservations.addAll(restaurantService.getRestaurantReservations(tripId));
						break;
					default:
						log.warn("지원하지 않는 예약 종류: {}", resKind);
						throw new IllegalArgumentException("지원하지 않는 예약 종류입니다: " + resKind);
				}
			}

			// 비즈니스 규칙: 날짜순으로 정렬
			allReservations.sort((a, b) -> a.getDate().compareTo(b.getDate()));

			// 페이지네이션 적용
			int totalElements = allReservations.size();
			int start = (int)pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), totalElements);

			List<ReservationItemResponseDto> pageContent = new ArrayList<>();
			if (start < totalElements) {
				pageContent = allReservations.subList(start, end);
			}

			Page<ReservationItemResponseDto> result = new PageImpl<>(pageContent, pageable, totalElements);

			log.info("예약 내역 조회 완료 - 총 {}건, 현재 페이지: {}/{}, 페이지 내용: {}건",
				totalElements,
				pageable.getPageNumber() + 1,
				result.getTotalPages(),
				pageContent.size());

			return result;

		} catch (Exception e) {
			log.error("예약 내역 조회 중 오류 발생 - tripId: {}, resKind: {}", tripId, resKind, e);
			throw new RuntimeException("예약 내역 조회 중 오류가 발생했습니다.", e);
		}
	}

	@Override
	public Page<ReservationItemResponseDto> getReservationsByDate(Long memberId, Long tripId, LocalDate date,
		Pageable pageable) {
		log.info("날짜별 예약 내역 조회 시작 - memberId: {}, tripId: {}, date: {}, page: {}, size: {}",
			memberId, tripId, date, pageable.getPageNumber(), pageable.getPageSize());

		List<ReservationItemResponseDto> allReservations = new ArrayList<>();

		try {
			// 각 서비스에서 날짜별, 멤버별 예약 내역 조회
			allReservations.addAll(transportService.getTransportReservationsByDateAndMember(memberId, tripId, date));
			allReservations.addAll(
				accommodationService.getAccommodationReservationsByDateAndMember(memberId, tripId, date));
			allReservations.addAll(restaurantService.getRestaurantReservationsByDateAndMember(memberId, tripId, date));

			// 예매시간 순으로 정렬 (createdAt 기준)
			allReservations.sort((a, b) -> {
				if (a.getCreatedAt() == null && b.getCreatedAt() == null)
					return 0;
				if (a.getCreatedAt() == null)
					return 1;
				if (b.getCreatedAt() == null)
					return -1;
				return a.getCreatedAt().compareTo(b.getCreatedAt());
			});

			// 페이지네이션 적용
			int totalElements = allReservations.size();
			int start = (int)pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), totalElements);

			List<ReservationItemResponseDto> pageContent = new ArrayList<>();
			if (start < totalElements) {
				pageContent = allReservations.subList(start, end);
			}

			Page<ReservationItemResponseDto> result = new PageImpl<>(pageContent, pageable, totalElements);

			log.info("날짜별 예약 내역 조회 완료 - 총 {}건, 현재 페이지: {}/{}, 페이지 내용: {}건",
				totalElements,
				pageable.getPageNumber() + 1,
				result.getTotalPages(),
				pageContent.size());

			return result;

		} catch (Exception e) {
			log.error("날짜별 예약 내역 조회 중 오류 발생 - memberId: {}, tripId: {}, date: {}", memberId, tripId, date, e);
			throw new RuntimeException("날짜별 예약 내역 조회 중 오류가 발생했습니다.", e);
		}
	}
}
