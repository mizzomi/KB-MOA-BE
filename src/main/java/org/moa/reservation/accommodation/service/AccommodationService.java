package org.moa.reservation.accommodation.service;

import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AccommodationService {
    Page<AccommodationInfoResponse> searchAccommodations(LocalDateTime checkinDay, LocalDateTime checkoutDay, Pageable pageable);
}
