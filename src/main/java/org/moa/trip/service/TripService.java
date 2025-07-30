package org.moa.trip.service;

import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public interface TripService {
    public boolean createTrip(TripCreateRequestDto dto);
    Page<TripListResponseDto> getTripList(Long memberId, Pageable pageable);
}
