package org.moa.trip.controller;

import com.google.protobuf.Api;
import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.member.entity.Member;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.moa.trip.service.TripRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/trips/{tripId}/records")
@RequiredArgsConstructor
public class TripRecordController {

    private final TripRecordService tripRecordService;

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
}
