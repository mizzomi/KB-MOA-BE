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
import org.moa.reservation.transport.mapper.TransportMapper;
import org.moa.reservation.transport.type.Status;
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

	private final ReservationMapper reservationMapper;
	private final TransportMapper transportMapper;
	private final TripMapper tripMapper;

	@Override
	public Map<Integer, List<TransSeatsInfoResponse>> getSeats(Long transportId) {
		// 목록 조회
		List<TransSeatsInfoResponse> transportSeatsInfos = transportMapper.selectSeatsByTransportId(transportId);

		// seatRoomNo 기준으로 묶어서 LinkedHashMap 으로 반환
		return transportSeatsInfos.stream()
			.collect(Collectors.groupingBy(
				TransSeatsInfoResponse::getSeatRoomNo,
				LinkedHashMap::new,       // insertion-order 유지
				Collectors.toList()
			));
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

		//전체 건수 조회
		int total = transportMapper.countTransports(
			departureName, destinationName, departureDateTime
		);

		//Spring Data의 PageImpl로 감싸서 반환
		return new PageImpl<>(transportInfos, pageable, total);
	}

	@Transactional
	public Long reserveTransportSeats(TransResRequestDto dto) {
		// 1. trip_day_id 조회
		Long tripDayId = tripMapper.findTripDayId(dto.getTripId(), dto.getDepartureDateTime().toLocalDate());
		if(tripDayId == null) {
			throw new IllegalArgumentException("해당 날짜의 trip_day가 존재하지 않습니다.");
		}

		//2. 좌석 상태 확인
		log.info("업데이트할 tran_res_id 목록: {}", dto.getTranResIds());
		List<TransResStatusDto> seatStatuses = transportMapper.findStatusesByIds(dto.getTranResIds());

		boolean allAvailable = seatStatuses.stream()
			.allMatch(seat -> seat.getStatus() == Status.AVAILABLE);
		if(!allAvailable) {
			throw new IllegalStateException("선택한 좌석 중 이미 예약 중인 좌석이 있습니다.");
		}

		List<Long> inputIds = dto.getTranResIds();
		List<Long> existingIds = transportMapper.findExistingTranResIds(inputIds);

		if (existingIds.size() != inputIds.size()) {
			// 누락된 ID 확인
			List<Long> missingIds = new ArrayList<>(inputIds);
			missingIds.removeAll(existingIds);


			throw new IllegalArgumentException("선택한 좌석 중 존재하지 않는 좌석이 있습니다. 다시 확인해 주세요.");
		}

		//3. Reservation 저장
		Reservation reservation = Reservation.builder()
			.tripDayId(tripDayId)
			.resKind(ResKind.TRANSPORT)
			.build();
		reservationMapper.insertReservation(reservation);
		Long reservationId = reservation.getReservationId();

		//4. 좌석들 업데이트
		int updatedCount = transportMapper.updateSeatsToPending(
			reservationId,
			tripDayId,
			dto.getTranResIds(),
			LocalDateTime.now()
		);
		log.info("예약 상태로 변경된 좌석 수: {}", updatedCount);

		log.info("TransportService.reserveTransportSeats reservationId={} ==== 좌석 예약 완료(결제전)", reservationId);
		return reservationId;
	}

	@Transactional
	public Boolean seatPayment(Long memberId, TransPaymentRequestDto dto) {
		Long reservationId = dto.getReservationId();
		BigDecimal amount = dto.getPrice();

		Long ownerMemberId = reservationMapper.findMemberIdByReservationId(reservationId);

		if (ownerMemberId == null) {
			throw new IllegalArgumentException("해당 reservationId [" + reservationId + "] 에 해당하는 예약이 존재하지 않습니다.");
		}
		if (!ownerMemberId.equals(memberId)) {
			throw new IllegalStateException("해당 사용자의 예약건이 아닙니다.");
		}

		BigDecimal totalPrice = transportMapper.getTotalPriceByReservationId(reservationId);
		if(totalPrice.compareTo(dto.getPrice()) != 0) {
			throw new IllegalArgumentException("입력된 금액이 실제 결제될 금액과 다릅니다.");
		}

		String trainNo = transportMapper.selectTrainNoByReservationId(reservationId);

		//실질적인 결제 프로세스 -> 향후 외부 PG 연동 가능성 확보
		PaymentResponseDto result = accountService.makePayment(memberId, amount, trainNo);

		int updated = transportMapper.confirmSeatsByReservationId(reservationId);
		if(updated == 0) {
			throw new IllegalStateException("예약확정으로 변경된 좌석이 없습니다. 다시 시도해주세요.");
		}

		return true;
	}

	@Transactional
	public int cancelReservation(Long memberId, TransResCancelRequestDto dto) {
		Long reservationId = dto.getReservationId();

		//예약 소유자 확인
		Long ownerMemberId = reservationMapper.findMemberIdByReservationId(reservationId);
		if(ownerMemberId == null || !ownerMemberId.equals(memberId)) {
			throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다.");
		}

		// 좌석 상태 초기화 (단, status = 'PENDING'인 것만)
		int cancelSeats = transportMapper.cancelSeatsByReservationId(reservationId);
		if (cancelSeats == 0) {
			throw new IllegalStateException("취소 가능한 좌석이 없습니다. 이미 결제되었거나 취소된 상태입니다.");
		}

		// 예약 정보 삭제
		int cancelReservation = reservationMapper.cancelReservationByReservationId(reservationId);

		log.info("TransportService.cancelReservation: memberId = {} }의 예약을 취소하였습니다.", memberId);
		return cancelSeats;
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
