package org.moa.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrTransportReservationDto {
    private String type = "transport"; // 교통 예약
    private Long reservationId;
    private Long transportId;
    private String departure;
    private String arrival;
    private Integer seatRoomNo;
    private String seatNumber;
    private String seatType;
    private String status;
}