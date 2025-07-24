package org.moa.trip.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDto {
    private Long expenseId;
    private LocalDateTime expenseDate;
    private BigDecimal shareAmount;
    private Boolean received;
    private String status;
}
