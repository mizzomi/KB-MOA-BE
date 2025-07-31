package org.moa.reservation.transport.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.moa.reservation.transport.dto.TransPaymentRequestDto;
import org.moa.reservation.transport.dto.TransportInfoResponse;
import org.moa.reservation.transport.dto.TransportReservationRequestDto;
import org.moa.reservation.transport.dto.TransportSeatsInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransportService {
	Page<TransportInfoResponse> searchTransports(
		String departureName,
		String destinationName,
		LocalDateTime departureDateTime,
		Pageable pageable
	);

	Map<Integer, List<TransportSeatsInfoResponse>> getSeats(Long transportId);

	Long reserveTransportSeats(TransportReservationRequestDto dto);

	Boolean seatPayment(Long memberId, TransPaymentRequestDto dto);
}
