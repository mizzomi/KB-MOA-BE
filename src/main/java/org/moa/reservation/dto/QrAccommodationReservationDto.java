package org.moa.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrAccommodationReservationDto {
    private String type; // 숙박 예약
    private Long reservationId;
    private Long accomId;
    private String hotelName;
    private String roomType;
    private String checkinDay;
    private String checkoutDay;
    private Integer guests;
    private String status;
}