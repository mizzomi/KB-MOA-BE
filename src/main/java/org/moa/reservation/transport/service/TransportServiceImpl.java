package org.moa.reservation.transport.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.account.service.AccountService;
import org.moa.global.type.ResKind;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.entity.Reservation;
import org.moa.reservation.mapper.ReservationMapper;
import org.moa.reservation.transport.dto.TransResCancelRequestDto;
import org.moa.reservation.transport.dto.TransResStatusDto;
import org.moa.reservation.transport.dto.TransPaymentRequestDto;
import org.moa.reservation.transport.dto.TranstInfoResponse;
import org.moa.reservation.transport.dto.TransResRequestDto;
import org.moa.reservation.transport.dto.TransSeatsInfoResponse;
import org.moa.reservation.transport.exception.TransportReservationException;
import org.moa.reservation.transport.mapper.TransportMapper;
import org.moa.reservation.transport.type.Status;
import org.moa.reservation.transport.validator.TransportReservationValidator;
import org.moa.reservation.transport.helper.SeatGroupingHelper;
import org.moa.trip.mapper.TripMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportServiceImpl implements TransportService {

	private final AccountService accountService;
	private final TransportReservationValidator validator;
	private final SeatGroupingHelper seatGroupingHelper;

	private final ReservationMapper reservationMapper;
	private final TransportMapper transportMapper;
	private final TripMapper tripMapper;

	@Override
	public Map<Integer, List<TransSeatsInfoResponse>> getSeats(Long transportId) {
		// 목록 조회
		List<TransSeatsInfoResponse> transportSeatsInfos = transportMapper.selectSeatsByTransportId(transportId);
		
		// 헬퍼 클래스를 사용하여 그룹화
		return seatGroupingHelper.groupSeatsByRoom(transportSeatsInfos);
	}

	@Override
	public Page<TranstInfoResponse> searchTransports(
		String departureName,
		String destinationName,
		LocalDateTime departureDateTime,
		Pageable pageable
	) {
		// 목록 조회
		List<TranstInfoResponse> transportInfos = transportMapper.selectTransports(
			departureName, destinationName, departureDateTime, pageable.getPageNumber() * pageable.getPageSize(), pageable.getPageSize()
		);

		// 전체 건수 조회
		int total = transportMapper.countTransports(
			departureName, destinationName, departureDateTime
		);

		// Spring Data의 PageImpl로 감싸서 반환
		return new PageImpl<>(transportInfos, pageable, total);
	}

	@Transactional
	public Long reserveTransportSeats(TransResRequestDto dto) {
		// 1. trip_day_id 조회
		Long tripDayId = findTripDayIdOrThrow(dto.getTripId(), dto.getDepartureDateTime());

		// 2. 좌석 유효성 검증 (Validator에 위임)
		validator.validateSeatsForReservation(dto.getTranResIds());

		// 3. Reservation 저장
		Long reservationId = createReservation(tripDayId);

		// 4. 좌석들 업데이트
		updateSeatsToPending(reservationId, tripDayId, dto.getTranResIds());
		
		log.info("TransportService.reserveTransportSeats reservationId={} ==== 좌석 예약 완료(결제전)", reservationId);
		return reservationId;
	}
	
	// Trip Day ID 조회 헬퍼 메서드
	private Long findTripDayIdOrThrow(Long tripId, LocalDateTime departureDateTime) {
		Long tripDayId = tripMapper.findTripDayId(tripId, departureDateTime.toLocalDate());
		if (tripDayId == null) {
			throw TransportReservationException.tripDayNotFound();
		}
		return tripDayId;
	}
	
	// Reservation 생성 헬퍼 메서드
	private Long createReservation(Long tripDayId) {
		Reservation reservation = Reservation.builder()
			.tripDayId(tripDayId)
			.resKind(ResKind.TRANSPORT)
			.build();
		reservationMapper.insertReservation(reservation);
		return reservation.getReservationId();
	}
	
	// 좌석 상태 업데이트 헬퍼 메서드
	private void updateSeatsToPending(Long reservationId, Long tripDayId, List<Long> tranResIds) {
		int updatedCount = transportMapper.updateSeatsToPending(
			reservationId,
			tripDayId,
			tranResIds,
			LocalDateTime.now()
		);
		log.info("예약 상태로 변경된 좌석 수: {}", updatedCount);
	}

	@Transactional
	public Boolean seatPayment(Long memberId, TransPaymentRequestDto dto) {
		Long reservationId = dto.getReservationId();
		BigDecimal amount = dto.getPrice();

		// 1. 예약 소유권 검증
		validator.validateReservationOwnershipForPayment(memberId, reservationId);

		// 2. 결제 금액 검증
		BigDecimal totalPrice = transportMapper.getTotalPriceByReservationId(reservationId);
		validator.validatePaymentAmount(totalPrice, amount);

		// 3. 결제 처리
		processPayment(memberId, amount, reservationId);

		// 4. 좌석 상태 확정
		confirmSeats(reservationId);

		return true;
	}
	
	// 결제 처리 헬퍼 메서드
	private void processPayment(Long memberId, BigDecimal amount, Long reservationId) {
		String trainNo = transportMapper.selectTrainNoByReservationId(reservationId);
		
		// 실질적인 결제 프로세스 -> 향후 외부 PG 연동 가능성 확보
		PaymentResponseDto result = accountService.makePayment(memberId, amount, trainNo);
		log.info("결제 완료: reservationId={}, amount={}", reservationId, amount);
	}
	
	// 좌석 확정 처리 헬퍼 메서드
	private void confirmSeats(Long reservationId) {
		int updated = transportMapper.confirmSeatsByReservationId(reservationId);
		if (updated == 0) {
			throw TransportReservationException.paymentFailed();
		}
		log.info("좌석 확정 완료: reservationId={}, 확정 좌석 수={}", reservationId, updated);
	}

	@Transactional
	public int cancelReservation(Long memberId, TransResCancelRequestDto dto) {
		Long reservationId = dto.getReservationId();

		// 1. 예약 소유자 확인
		validator.validateReservationOwnershipForCancel(memberId, reservationId);

		// 2. 좌석 상태 초기화 (단, status = 'PENDING'인 것만)
		int cancelSeats = cancelSeats(reservationId);

		// 3. 예약 정보 삭제
		deleteReservation(reservationId);

		log.info("TransportService.cancelReservation: memberId = {} }의 예약을 취소하였습니다.", memberId);
		return cancelSeats;
	}
	
	// 좌석 취소 처리 헬퍼 메서드
	private int cancelSeats(Long reservationId) {
		int cancelSeats = transportMapper.cancelSeatsByReservationId(reservationId);
		if (cancelSeats == 0) {
			throw TransportReservationException.noCancellableSeats();
		}
		return cancelSeats;
	}
	
	// 예약 삭제 처리 헬퍼 메서드
	private void deleteReservation(Long reservationId) {
		int cancelReservation = reservationMapper.cancelReservationByReservationId(reservationId);
		log.info("예약 삭제 완료: reservationId={}", reservationId);
	}

	@Override
	public List<ReservationItemResponseDto> getTransportReservations(Long tripId) {
		return transportMapper.getTransportReservationsByTripId(tripId);
	}

	@Override
	public List<ReservationItemResponseDto> getTransportReservationsByDateAndMember(Long memberId, Long tripId, java.time.LocalDate date) {
		return transportMapper.getTransportReservationsByDateAndMember(memberId, tripId, date);
	}
}
