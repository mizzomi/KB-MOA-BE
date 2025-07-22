package org.moa.trip.service;

import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.springframework.stereotype.Service;

@Service
public interface TripService {
    public boolean createTrip(TripCreateRequestDto dto);
}
