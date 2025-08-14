package org.moa.reservation.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableTimeResponseDto {
    private String time;
    private Long dailySlotId;
    private int maxNum;
    private int reservedNum;
    private int availableNum;
}