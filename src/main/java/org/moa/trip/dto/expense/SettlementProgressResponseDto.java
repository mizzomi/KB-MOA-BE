package org.moa.trip.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementProgressResponseDto {
    private String expenseName;
    private LocalDateTime expenseDate;
    private BigDecimal amount;
    private List<String> names;
    private List<String> statuses;
}
