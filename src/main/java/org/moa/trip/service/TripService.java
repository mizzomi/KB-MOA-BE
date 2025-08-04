package org.moa.trip.service;

import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.dto.trip.TripLocationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface TripService {
    Long createTrip(TripCreateRequestDto dto);
    Page<TripListResponseDto> getTripList(Long memberId, String locationName, Pageable pageable);
    List<TripLocationResponseDto> getTripLocations();
}
