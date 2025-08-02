package org.moa.reservation.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.moa.reservation.transport.dto.TransPaymentRequestDto;
import org.moa.reservation.transport.dto.TransResCancelRequestDto;
import org.moa.reservation.transport.dto.TranstInfoResponse;
import org.moa.reservation.transport.dto.TransResRequestDto;
import org.moa.reservation.transport.dto.TransSeatsInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransportService {
	Page<TranstInfoResponse> searchTransports(
		String departureName,
		String destinationName,
		LocalDateTime departureDateTime,
		Pageable pageable
	);

	Map<Integer, List<TransSeatsInfoResponse>> getSeats(Long transportId);

	Long reserveTransportSeats(TransResRequestDto dto);

	Boolean seatPayment(Long memberId, TransPaymentRequestDto dto);

	int cancelReservation(Long memberId, TransResCancelRequestDto dto);
}
