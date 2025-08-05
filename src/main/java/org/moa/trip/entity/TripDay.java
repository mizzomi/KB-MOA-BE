package org.moa.trip.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripDay {
    private Long tripDayId;
    private Long tripId;
    private LocalDate day;
}
