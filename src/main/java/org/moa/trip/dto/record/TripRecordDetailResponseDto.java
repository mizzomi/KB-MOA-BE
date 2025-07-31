package org.moa.trip.dto.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.moa.trip.entity.TripRecord;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TripRecordDetailResponseDto {
    private Long recordId;
    private String title;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate recordDate;
    private String content; // 내용 (Nullable)
    private List<String> imageUrls; // 이미지 URL 목록 (Nullable)

    public static TripRecordDetailResponseDto of(TripRecord tripRecord, List<String> imageUrls) {
        return TripRecordDetailResponseDto.builder()
                .recordId(tripRecord.getRecordId())
                .title(tripRecord.getTitle())
                .recordDate(tripRecord.getRecordDate())
                .content(tripRecord.getContent())
                .imageUrls(imageUrls)
                .build();
    }
}