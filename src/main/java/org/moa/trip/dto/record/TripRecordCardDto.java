package org.moa.trip.dto.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class TripRecordCardDto {
    private Long recordId;
    private String title;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    // 한 기록에 속한 여러 이미지 URL을 담을 리스트
    private List<String> imageUrls;
}
