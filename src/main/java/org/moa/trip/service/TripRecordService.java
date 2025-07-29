package org.moa.trip.service;

import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;

public interface TripRecordService {
    TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto);


}
