package org.moa.trip.service;

import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.TripRecordDetailResponseDto;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TripRecordService {
    TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto);

    Page<TripRecordCardDto> getRecordsByDate(Long tripId, LocalDate date, Pageable pageable);

    TripRecordDetailResponseDto getRecordDetail(Long tripId, Long recordId);
}
