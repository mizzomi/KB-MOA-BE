package org.moa.trip.service;

import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.TripRecordDetailResponseDto;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TripRecordService {
    // 여행 기록 생성
    TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto);

    // 일자별 여행 기록 조회
    Page<TripRecordCardDto> getRecordsByDate(Long tripId, LocalDate date, Pageable pageable);

    // 여행 기록 상세 조회
    TripRecordDetailResponseDto getRecordDetail(Long tripId, Long recordId);

    // 여행 기록 수정
    TripRecordResponseDto updateRecord(Long tripId, Long recordId, Long memberId, TripRecordRequestDto dto);

    // 여행 기록 삭제
    void deleteRecord(Long tripId, Long recordId, Long memberId);
}
