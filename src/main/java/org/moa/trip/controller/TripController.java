package org.moa.trip.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.trip.dto.settlement.ExpenseCreateRequestDto;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.service.ExpenseService;
import org.moa.trip.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;
    private final ExpenseService expenseService;

    @PostMapping("/trip")
    public ResponseEntity<ApiResponse<?>> createTrip(@Valid @RequestBody TripCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(tripService.createTrip(dto)));
    }

    @PostMapping("/settlement")
    public ResponseEntity<ApiResponse<?>> createExpense(@Valid @RequestBody ExpenseCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(expenseService.createExpense(dto)));
    }
}
