package org.moa.trip.entity;

import lombok.*;
import org.moa.trip.type.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripLocation {
    private Long locationId;
    private Location locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
}
