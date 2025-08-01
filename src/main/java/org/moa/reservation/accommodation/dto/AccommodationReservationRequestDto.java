package org.moa.reservation.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationReservationRequestDto {
    private Long tripId;
    private Long accomResId;
    private LocalDate checkinDay;
    private LocalDate checkoutDay;
    private Integer guests;
    private BigDecimal price;
}
