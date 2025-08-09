package org.moa.trip.dto.record;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class TripRecordUpdateRequestDto {
    private String title;
    private String content;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    // 클라이언트가 유지하기를 원하는 기존 이미지의 파일 이름 목록
    private List<String> existingImageFileNames;

    // 새로 추가할 이미지 파일 목록
    private List<MultipartFile> newImages;
}