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
    private List<ImageInfo> images; // 이미지 정보 목록 (Nullable)

    // 이미지 정보를 구조화하여 전달하기 위한 내부 DTO
    @Data
    @AllArgsConstructor
    public static class ImageInfo {
        private String url;      // 화면 표시용 서명된 URL
        private String fileName; // 서버와 통신하기 위한 고유 파일 이름
    }

    public static TripRecordDetailResponseDto of(TripRecord tripRecord, List<ImageInfo> images) {
        return TripRecordDetailResponseDto.builder()
                .recordId(tripRecord.getRecordId())
                .title(tripRecord.getTitle())
                .recordDate(tripRecord.getRecordDate())
                .content(tripRecord.getContent())
                .images(images)
                .build();
    }
}