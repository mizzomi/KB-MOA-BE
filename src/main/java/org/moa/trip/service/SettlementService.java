package org.moa.trip.service;

import org.moa.trip.dto.settlement.SettlementProgressResponseDto;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface SettlementService {
    public boolean createSettlement(Long expenseId, Long tripId, Long creatorId, Long memberId, BigDecimal amount);

    public boolean settle(SettlementRequestDto dto);

    public SettlementProgressResponseDto getSettlementProgress(Long expenseId);
}
