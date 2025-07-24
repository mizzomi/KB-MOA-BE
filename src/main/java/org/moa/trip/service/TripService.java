package org.moa.trip.service;

import org.moa.trip.dto.trip.PageResponse;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TripService {
    public boolean createTrip(TripCreateRequestDto dto);
    PageResponse<TripListResponseDto> getTripList(Long memberId, int page, int size);
}
