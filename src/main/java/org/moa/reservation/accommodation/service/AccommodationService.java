package org.moa.reservation.accommodation.service;

import org.moa.reservation.accommodation.dto.AccommodationDetailResponse;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.reservation.accommodation.dto.AccommodationRoomsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AccommodationService {
    Page<AccommodationInfoResponse> searchAccommodations(Long tripId, LocalDate checkinDay, LocalDate checkoutDay, Pageable pageable);
    AccommodationDetailResponse getAccommodation(Long accomId);
    List<AccommodationRoomsResponse>  getRooms(Long accomId, Long tripId, LocalDate checkinDay, LocalDate checkoutDay, Integer guests);
}
