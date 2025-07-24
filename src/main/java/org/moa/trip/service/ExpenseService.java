package org.moa.trip.service;

import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.expense.ExpenseResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ExpenseService {
    public boolean createExpense(ExpenseCreateRequestDto dto);

    public List<ExpenseResponseDto> getExpenses(Long memberId, Long tripId);
}
