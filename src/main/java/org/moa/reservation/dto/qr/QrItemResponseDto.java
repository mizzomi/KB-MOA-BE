package org.moa.reservation.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QrItemResponseDto<T> {
    private String qrCodeString; // QR 값
    private T details; // 예약 정보
}
