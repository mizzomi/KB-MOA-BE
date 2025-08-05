package org.moa.reservation.service;

import org.moa.reservation.dto.ReservationItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationService {
	Page<ReservationItemResponseDto> getReservations(Long tripId, String resKind, Pageable pageable);
}
