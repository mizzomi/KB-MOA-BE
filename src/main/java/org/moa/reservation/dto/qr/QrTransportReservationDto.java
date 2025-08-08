package org.moa.reservation.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrTransportReservationDto {
    private Long tranResId; // 좌석 고유 ID
    private String type; // 교통 예약
    private Long reservationId;
    private Long transportId;
    private String departure;
    private String arrival;
    private Integer seatRoomNo;
    private String seatNumber;
    private String seatType;
    private String status;
}