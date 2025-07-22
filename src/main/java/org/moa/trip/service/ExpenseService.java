package org.moa.trip.service;

import org.moa.trip.dto.settlement.ExpenseCreateRequestDto;
import org.springframework.stereotype.Service;

@Service
public interface ExpenseService {
    public boolean createExpense(ExpenseCreateRequestDto dto);
}
