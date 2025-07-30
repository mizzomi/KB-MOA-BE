package org.moa.trip.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRecordImage {
    private Long imageId;             // 이미지 ID (PK)
    private Long recordId;            // 여행 기록 ID (FK)
    private String imageUrl;          // Firebase 업로드된 이미지 URL
    private LocalDateTime createdAt;  // 작성일시
    private LocalDateTime updatedAt;  // 수정일시
}
