package org.moa.reservation.transport.service;

import java.time.LocalDateTime;

import org.moa.reservation.transport.dto.TransportInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransportService {
	Page<TransportInfoResponse> searchTransports(
		String departureName,
		String destinationName,
		LocalDateTime departureDateTime,
		Pageable pageable
	);
}
