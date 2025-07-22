package org.moa.trip.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripRecords {
    private Long recordId;
    private Long tripId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 이미지 여러장
    private List<TripRecordsImages>  images;
}
