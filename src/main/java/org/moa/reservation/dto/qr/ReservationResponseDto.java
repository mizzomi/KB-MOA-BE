package org.moa.reservation.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponseDto { // 식당, 숙박
    private String qrCodeString; // QR 값
    private Object reservationDetails; // 예약 정보
}