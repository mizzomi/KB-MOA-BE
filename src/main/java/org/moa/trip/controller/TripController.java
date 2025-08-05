package org.moa.trip.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.member.service.MemberService;
import org.moa.global.security.domain.CustomUser;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.expense.ExpenseResponseDto;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.dto.trip.TripDetailResponseDto;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.service.ExpenseService;
import org.moa.trip.service.SettlementService;
import org.moa.trip.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TripController {
    private final MemberService memberService;
    private final TripService tripService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<?>> createTrip(@Valid @RequestBody TripCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(tripService.createTrip(dto)));
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<Page<TripListResponseDto>>> getTripList(@AuthenticationPrincipal CustomUser customUser,
                                                                              @RequestParam(required = false) String locationName,
                                                                              Pageable pageable
    ) {
        Long memberId = customUser.getMember().getMemberId();
        Page<TripListResponseDto> tripPage = tripService.getTripList(memberId, locationName, pageable);
        return ResponseEntity.ok(ApiResponse.of(tripPage));
    }

    @PostMapping("/expense")
    public ResponseEntity<ApiResponse<?>> createExpense(@Valid @RequestBody ExpenseCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(expenseService.createExpense(dto)));
    }

    @GetMapping("/expense")
    public ResponseEntity<ApiResponse<?>> getExpenses(
            @PageableDefault(sort = "expenseId") Pageable pageable,
            @RequestParam Long tripId) {
        Page<ExpenseResponseDto> page = expenseService.getExpenses(tripId,pageable);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(page));
    }

    @GetMapping("/settlement-progress")
    public ResponseEntity<ApiResponse<?>> getSettlementProgress(@RequestParam Long expenseId){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(settlementService.getSettlementProgress(expenseId)));
    }

    @GetMapping("/settlement")
    public ResponseEntity<ApiResponse<?>> settle(@RequestParam Long expenseId){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(settlementService.getSettlementInfo(expenseId)));
    }

    @PostMapping("/settlement")
    public ResponseEntity<ApiResponse<?>> settle(@RequestBody SettlementRequestDto dto){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(settlementService.settle(dto)));
    }

    @GetMapping("/trip-members")
    public ResponseEntity<ApiResponse<?>> getTripMembers(@RequestParam Long tripId){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.getTripMembers(tripId)));
    }

    @GetMapping("/trip-locations")
    public ResponseEntity<ApiResponse<?>> getTripLocations(){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(tripService.getTripLocations()));
    }

    /**
     * 여행 상세 정보 조회 API
     * @param tripId 여행 ID (필수)
     * @return 여행 상세 정보
     */
    @GetMapping("/trip-detail")
    public ResponseEntity<ApiResponse<TripDetailResponseDto>> getTripDetail(@RequestParam Long tripId) {
        try {
            TripDetailResponseDto tripDetail = tripService.getTripDetail(tripId);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(tripDetail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(org.moa.global.type.StatusCode.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.moa.global.type.StatusCode.INTERNAL_ERROR, "여행 상세 조회 중 오류가 발생했습니다."));
        }
    }
}
