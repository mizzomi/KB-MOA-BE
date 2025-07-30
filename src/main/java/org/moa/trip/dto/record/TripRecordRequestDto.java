package org.moa.trip.dto.record;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class TripRecordRequestDto {
    @NotBlank(message = "제목은 비워둘 수 없습니다.")
    private String title;

    @NotNull(message = "여행 날짜는 필수입니다.")
    private LocalDate recordDate;

    private String content;
    private List<String> imageUrls;
}
