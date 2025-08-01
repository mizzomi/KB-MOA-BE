package org.moa.reservation.accommodation.entity;

import lombok.*;
import org.moa.trip.type.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationInfo {
    private Long  accomId;
    private String hotelName;
    private String address;
    private Location location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;
    private String hotelImageUrl;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
