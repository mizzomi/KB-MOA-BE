package org.moa.trip.service;

import org.moa.trip.dto.settlement.SettlementProgressResponseDto;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface SettlementService {
    boolean createSettlement(Long expenseId, Long tripId, Long creatorId, Long memberId, BigDecimal amount);

    boolean settle(SettlementRequestDto dto);

    SettlementProgressResponseDto getSettlementProgress(Long expenseId);
}
