package org.moa.trip.dto.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInfoResponseDto {
    private String receiverName;
    private BigDecimal shareAmount;
    private BigDecimal balance;
}
