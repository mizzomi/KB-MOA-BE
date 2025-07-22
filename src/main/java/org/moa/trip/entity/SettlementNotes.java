package org.moa.trip.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SettlementNotes {
    private Long settlementId;
    private Long expenseId;
    private Long tripId;
    private Long memberId;
    private BigDecimal shareAmount;
    private Boolean received;
    private Boolean isPayed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
