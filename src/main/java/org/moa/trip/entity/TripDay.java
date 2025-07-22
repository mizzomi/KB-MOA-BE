package org.moa.trip.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripDay {
    private String tripDayId;
    private String tripId;
    private LocalDateTime day;
}
