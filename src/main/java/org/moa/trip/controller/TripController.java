package org.moa.trip.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.member.service.MemberService;
import org.moa.global.security.domain.CustomUser;
import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.trip.PageResponse;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.service.ExpenseService;
import org.moa.trip.service.SettlementService;
import org.moa.trip.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TripController {
    private final MemberService memberService;
    private final TripService tripService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;


    @PostMapping("/trip")
    public ResponseEntity<ApiResponse<?>> createTrip(@Valid @RequestBody TripCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(tripService.createTrip(dto)));
    }


    @GetMapping("/trip")
    public ResponseEntity<ApiResponse<PageResponse<TripListResponseDto>>> getTripList(@AuthenticationPrincipal CustomUser customUser,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = customUser.getMember().getMemberId();
        PageResponse<TripListResponseDto> tripPage = tripService.getTripList(memberId, page, size);
        return ResponseEntity.ok(ApiResponse.of(tripPage));
    }

    @PostMapping("/expense")
    public ResponseEntity<ApiResponse<?>> createExpense(@Valid @RequestBody ExpenseCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(expenseService.createExpense(dto)));
    }

    @GetMapping("/expense")
    public ResponseEntity<ApiResponse<?>> getExpenses(@RequestParam Long memberId,  @RequestParam Long tripId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(expenseService.getExpenses(memberId,tripId)));
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
}
