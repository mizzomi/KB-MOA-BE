package org.moa.trip.dto.expense;

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
    private Long tripId;
    private BigDecimal amount;
    private String expenseName;
    private List<AmountAndMemberIdRequest> expenses;
}
