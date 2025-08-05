package org.moa.trip.service;

import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.expense.ExpenseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ExpenseService {
    boolean createExpense(ExpenseCreateRequestDto dto);
    Page<ExpenseResponseDto> getExpenses(Long tripId, Pageable pageable);
}
