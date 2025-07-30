package org.moa.trip.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRecord {
    private Long recordId; // 기록 ID (PK)
    private Long tripId; // 여행 ID (FK)
    private String title; // 제목
    private LocalDate recordDate; // 기록 날짜
    private String content; // 내용
    private LocalDateTime createdAt; // 작성일시
    private LocalDateTime updatedAt; // 수정일시

    // 1:N 관계 - 이미지 리스트
    private List<TripRecordImage>  images;
}
