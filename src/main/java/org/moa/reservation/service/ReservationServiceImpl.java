package org.moa.reservation.service;

import java.util.ArrayList;
import java.util.List;

import org.moa.reservation.accommodation.service.AccommodationService;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.restaurant.service.RestaurantService;
import org.moa.reservation.transport.service.TransportService;
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
	public List<ReservationItemResponseDto> getReservations(Long tripId, String resKind) {
		log.info("예약 내역 조회 시작 - tripId: {}, resKind: {}", tripId, resKind);

		List<ReservationItemResponseDto> result = new ArrayList<>();

		try {
			if (resKind == null || resKind.trim().isEmpty()) {
				// 전체 조회 - 모든 예약 종류 조회
				log.debug("전체 예약 내역 조회");
				result.addAll(transportService.getTransportReservations(tripId));
				result.addAll(accommodationService.getAccommodationReservations(tripId));
				result.addAll(restaurantService.getRestaurantReservations(tripId));
			} else {
				// 특정 종류만 조회
				log.debug("특정 예약 종류 조회: {}", resKind);
				switch (resKind.toUpperCase().trim()) {
					case "TRANSPORT":
						result.addAll(transportService.getTransportReservations(tripId));
						break;
					case "ACCOMMODATION":
						result.addAll(accommodationService.getAccommodationReservations(tripId));
						break;
					case "RESTAURANT":
						result.addAll(restaurantService.getRestaurantReservations(tripId));
						break;
					default:
						log.warn("지원하지 않는 예약 종류: {}", resKind);
						throw new IllegalArgumentException("지원하지 않는 예약 종류입니다: " + resKind);
				}
			}

			// 비즈니스 규칙: 날짜순으로 정렬
			result.sort((a, b) -> a.getDate().compareTo(b.getDate()));

			log.info("예약 내역 조회 완료 - 총 {}건", result.size());
			return result;

		} catch (Exception e) {
			log.error("예약 내역 조회 중 오류 발생 - tripId: {}, resKind: {}", tripId, resKind, e);
			throw new RuntimeException("예약 내역 조회 중 오류가 발생했습니다.", e);
		}
	}
}
