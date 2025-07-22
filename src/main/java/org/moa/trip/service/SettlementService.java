package org.moa.trip.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface SettlementService {
    public boolean createSettlement(Long expenseId, Long tripId, Long memberId, BigDecimal amount);
}
