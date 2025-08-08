package org.moa.trip.dto.record;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class TripRecordRequestDto {
    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotNull(message = "여행 날짜를 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    private String content;
    private List<MultipartFile> imageUrls;
}
