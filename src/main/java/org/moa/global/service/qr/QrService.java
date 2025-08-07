package org.moa.global.service.qr;

import org.moa.member.dto.qr.IdCardResponseDto;

public interface QrService {

    // 주민등록증 QR 생성 및 암호화
    String generateIdCardQr(Long memberId) throws Exception;

    // 주민등록증 QR 복호화 및 정보 조회
    IdCardResponseDto decryptIdCardQr(String encryptedText);

    // 예약 내역 QR 생성 및 암호화
    String generateReservationQr(Long reservationId, Long memberId) throws Exception;

    // 예약 상세 정보 조회
    Object getReservationInfo(Long reservationId, Long memberId);

    // 예약 내역 QR 복호화 및 정보 조회
    Object decryptReservationQr(String encryptedText, Long ownerId);
}
