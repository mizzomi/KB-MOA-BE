package org.moa.trip.controller;

import com.google.protobuf.Api;
import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.member.entity.Member;
import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.TripRecordDetailResponseDto;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.moa.trip.service.TripRecordService;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/trips/{tripId}/records")
@RequiredArgsConstructor
public class TripRecordController {

    private final TripRecordService tripRecordService;

    // 여행 기록 생성
    @PostMapping
    public ResponseEntity<ApiResponse<TripRecordResponseDto>> createTripRecord(
            @PathVariable Long tripId,
            @Valid @RequestBody TripRecordRequestDto requestDto,
            @AuthenticationPrincipal CustomUser loginUser) {

        Long memberId = loginUser.getMember().getMemberId();

        TripRecordResponseDto createdRecord = tripRecordService.createRecord(tripId, memberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(createdRecord, "여행 기록이 성공적으로 생성되었습니다."));
    }

    // 일자별 여행 기록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripRecordCardDto>>> getTripRecordsByDate(
            @PathVariable Long tripId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate date,
            @PageableDefault(size = 10, sort = "recordId", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TripRecordCardDto> recordPage = tripRecordService.getRecordsByDate(tripId, date, pageable);
        return ResponseEntity.ok(ApiResponse.of(recordPage));
    }

    // 여행 기록 상세 조회
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<TripRecordDetailResponseDto>> getTripRecordDetail(
            @PathVariable Long tripId,
            @PathVariable Long recordId
    ) {
        TripRecordDetailResponseDto recordDetail = tripRecordService.getRecordDetail(tripId, recordId);
        return ResponseEntity.ok(ApiResponse.of(recordDetail));
    }
}
