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
}
