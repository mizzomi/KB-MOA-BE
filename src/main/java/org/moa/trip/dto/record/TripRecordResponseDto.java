package org.moa.trip.dto.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import org.moa.trip.entity.TripRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TripRecordResponseDto {
    private Long recordId;
    private Long tripId;
    private String title;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate recordDate;
    private String content;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // 엔티티 객체를 DTO로 변환해주는 정적 메소드
    public static TripRecordResponseDto fromEntity(TripRecord entity) {
        return TripRecordResponseDto.builder()
                .recordId(entity.getRecordId())
                .tripId(entity.getTripId())
                .title(entity.getTitle())
                .recordDate(entity.getRecordDate())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
