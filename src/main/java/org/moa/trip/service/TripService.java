package org.moa.trip.service;

import org.moa.trip.dto.trip.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface TripService {
    Long createTrip(TripCreateRequestDto dto);
    boolean addMemberToTrip(AddMemberRequestDto dto);
    Page<TripListResponseDto> getTripList(Long memberId, String locationName, Pageable pageable);
    List<TripLocationResponseDto> getTripLocations();
    TripDetailResponseDto getTripDetail(Long tripId);
}
