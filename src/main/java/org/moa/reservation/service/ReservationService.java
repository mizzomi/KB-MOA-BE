package org.moa.reservation.service;

import java.util.List;

import org.moa.reservation.dto.ReservationItemResponseDto;

public interface ReservationService {
	List<ReservationItemResponseDto> getReservations(Long tripId, String resKind);
}
