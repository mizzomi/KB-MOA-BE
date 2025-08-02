package org.moa.reservation.accommodation.entity;

import lombok.*;
import org.moa.reservation.transport.type.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccomRes {
    private Long accomResId;
    private Long accomId;
    private Long reservationId;
    private Long tripDayId;
    private Integer guests;
    private String hotelName;
    private String address;
    private BigDecimal price;
    private String roomType;
    private String roomImageUrl;
    private LocalDateTime checkinDay;
    private LocalDateTime checkoutDay;
    private Integer maxGuests;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
