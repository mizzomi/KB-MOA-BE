package org.moa.trip.dto.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateRequestDto {
    private Long memberId;
    private Long tripId;
    private BigDecimal amount;
    private String expenseName;
    private List<Long> memberIds;
    private List<BigDecimal> amounts;
}
