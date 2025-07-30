package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripRecord;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TripRecordMapper {
    void insertTripRecord(TripRecord record);
    TripRecord findRecordById(Long recordId);

    // 특정 날짜의 여행 기록 목록을 페이지네이션과 함께 조회하는 메소드
    List<TripRecordCardDto> findRecordsByDate(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date,
            @Param("pageable") Pageable pageable
    );
    // 페이지네이션을 위한 전체 개수 카운트 메소드
    int countRecordsByDate(
            @Param("tripId") Long tripId,
            @Param("date") LocalDate date);

    // 특정 여행 기록에 속한 이미지 URL 목록 조회
    List<String> findImageUrlsByRecordId(Long recordId);

    // 여행ID + 기록ID 모두 만족하는 여행기록만 조회
    TripRecord findRecordByTripIdAndRecordId(@Param("tripId") Long tripId, @Param("recordId") Long recordId);
}
