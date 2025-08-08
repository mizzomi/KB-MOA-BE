package org.moa.reservation.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransportSeatResponseDto { // 교통
    private String qrCodeString; // QR 값
    private UserTransportReservationDto seatDetails; // 예약 정보
}
