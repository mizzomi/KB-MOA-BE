package org.moa.trip.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.account.dto.payment.LinkPaymentRecordsToTripDto;
import org.moa.global.account.service.AccountService;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@RestController
@RequestMapping("/api/trips/{tripId}/records")
@RequiredArgsConstructor
public class TripRecordController {

    private final TripRecordService tripRecordService;

    private final AccountService accountService;

    /** 여행 기록 생성 **/
    @PostMapping
    public ResponseEntity<ApiResponse<TripRecordResponseDto>> createTripRecord(
            @PathVariable Long tripId,
            @Valid @ModelAttribute TripRecordRequestDto requestDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUser loginUser) {

        // 유효성 검사 에러 처리
        ResponseEntity<ApiResponse<TripRecordResponseDto>> errorResponse = handleValidationErrors(bindingResult);
        if (errorResponse != null) return errorResponse;

        Long memberId = loginUser.getMember().getMemberId();

        TripRecordResponseDto createdRecord = tripRecordService.createRecord(tripId, memberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(createdRecord, "여행 기록이 성공적으로 생성되었습니다."));
    }

    /** 일자별 여행 기록 조회 **/
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripRecordCardDto>>> getTripRecordsByDate(
            @PathVariable Long tripId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate date,
            @PageableDefault(size = 10, sort = "recordId", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TripRecordCardDto> recordPage = tripRecordService.getRecordsByDate(tripId, date, pageable);
        return ResponseEntity.ok(ApiResponse.of(recordPage));
    }

    /** 여행 기록 상세 조회 **/
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<TripRecordDetailResponseDto>> getTripRecordDetail(
            @PathVariable Long tripId,
            @PathVariable Long recordId
    ) {
        TripRecordDetailResponseDto recordDetail = tripRecordService.getRecordDetail(tripId, recordId);
        return ResponseEntity.ok(ApiResponse.of(recordDetail));
    }

    /** 여행 기록 수정 **/
    @PutMapping("/{recordId}")
    public ResponseEntity<ApiResponse<TripRecordResponseDto>> updateTripRecord(
            @PathVariable Long tripId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal CustomUser loginUser,
            @Valid @ModelAttribute TripRecordRequestDto requestDto,
            BindingResult bindingResult) {

        // 유효성 검사 에러 처리
        ResponseEntity<ApiResponse<TripRecordResponseDto>> errorResponse = handleValidationErrors(bindingResult);
        if (errorResponse != null) return errorResponse;

        Long memberId = loginUser.getMember().getMemberId();
        TripRecordResponseDto updatedRecord = tripRecordService.updateRecord(tripId, recordId, memberId, requestDto);
        return ResponseEntity.ok(ApiResponse.of(updatedRecord, "여행 기록이 성공적으로 수정되었습니다."));
    }

    /** 여행 기록 삭제 **/
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<String>> deleteTripRecord(
            @PathVariable Long tripId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal CustomUser loginUser) {

        Long memberId = loginUser.getMember().getMemberId();
        tripRecordService.deleteRecord(tripId, recordId, memberId);
        return ResponseEntity.ok(ApiResponse.of("여행 기록이 성공적으로 삭제되었습니다."));
    }


    /** @Valid에 의한 유효성 검사 실패 시, 에러 응답을 생성하는 헬퍼 메서드 **/
    private <T> ResponseEntity<ApiResponse<T>> handleValidationErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<ApiResponse.FieldError> errors = bindingResult.getFieldErrors().stream()
                    .map(error -> ApiResponse.FieldError.builder()
                            .field(error.getField())
                            .reason(error.getDefaultMessage())
                            .build())
                    .collect(Collectors.toList());

            ApiResponse<T> errorBody = ApiResponse.error(StatusCode.INVALID_INPUT, errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        }
        return null;
    }

    @GetMapping("/payment-records")
    public ResponseEntity<ApiResponse<?>>  getPaymentRecords(
            @PathVariable Long tripId
    ){
        System.out.println("log : "+ tripId.toString());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(accountService.getPaymentRecords(tripId)));
    }

    @PostMapping("/payment-records")
    public ResponseEntity<ApiResponse<?>> LinkPaymentRecordToTrip(
            @PathVariable Long tripId,
            @RequestBody LinkPaymentRecordsToTripDto dto
    ){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(accountService.LinkPaymentRecordToTrip(tripId,dto)));
    }

    @GetMapping("/linked-records")
    public ResponseEntity<ApiResponse<?>>  getLinkedRecords(
            @PathVariable Long tripId
    ){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(accountService.getLinkedRecords(tripId)));
    }
}
