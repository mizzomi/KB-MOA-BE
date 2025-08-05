package org.moa.global.service.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.util.AesUtil;
import org.moa.global.util.QrCodeUtil;
import org.moa.member.dto.qr.IdCardResponseDto;
import org.moa.member.entity.IdCard;
import org.moa.member.mapper.IdCardMapper;
import org.moa.reservation.dto.QrRestaurantReservationDto;
import org.moa.reservation.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService{

    private final IdCardMapper idCardMapper;
    private final ReservationMapper reservationMapper;

    // 주민등록증 QR 생성 API
    @Override
    public String generateIdCardQr(Long memberId) {
        try {
            // ========= 테스트용 =========
            // memberId가 1이면 고정된 QR Base64 반환
            if (memberId == 1) {
                Map<String, Long> info = new HashMap<>();
                info.put("member_id", memberId);
                String json = new ObjectMapper().writeValueAsString(info);

                String encrypted = AesUtil.encryptWithIv(json);

                log.info("🔐 Postman 테스트용 data 파라미터: {}", encrypted);

                return QrCodeUtil.generateEncryptedQr(json);
            }

            // 1. DB에서 해당 memberId의 주민등록증 정보 존재 확인
            IdCard card = idCardMapper.findByMemberId(memberId);
            if(card == null) {
                throw new NoSuchElementException("해당 회원의 주민등록증 정보가 없습니다.");
            }

            // 2. QR 생성
            Map<String, Long> info = new HashMap<>();
            info.put("member_id", memberId); // QR 안에 들어갈 단일 값 : member_id

            String json = new ObjectMapper().writeValueAsString(info); // json = "{\"member_id\":1}" 로 직렬화

            return QrCodeUtil.generateEncryptedQr(json);

        } catch (Exception e) {
            log.error("QR 생성 실패", e);
            throw new RuntimeException("QR 생성 실패: " + e.getMessage());
        }
    }

    // 주민등록증 QR 복호화 API
    @Override
    public IdCardResponseDto decryptIdCardQr(String encryptedText) {
        try {
            String json = AesUtil.decryptWithIv(encryptedText);
            Map<String, Object> data = new ObjectMapper().readValue(json, Map.class); // json -> map 으로 파싱

            Long memberId = Long.valueOf(data.get("member_id").toString());
            IdCard card = idCardMapper.findByMemberId(memberId); // memberId를 통해 DB 조회해서 정보 가져옴

            if (card == null) {
                throw new NoSuchElementException("해당 회원의 주민등록증 정보가 없습니다.");
            } // Controller에서 404 응답 처리

            return new IdCardResponseDto(
                    card.getName(),
                    card.getIdCardNumber(),
                    card.getIssuedDate().toString(),
                    card.getAddress(),
                    card.getImageUrl()
            );

        } catch (Exception e) {
            throw new RuntimeException("복호화 실패 : " + e.getMessage());
        } // 복호화/파싱 실패 시 500 error
    }

    // 예약 내역 QR 생성 API
    @Override
    public String generateReservationQr(Long reservationId) {
        try {
            // 1. DB 조회
            QrRestaurantReservationDto reservation = reservationMapper.findQrInfoByReservationId(reservationId);
            if (reservation == null) {
                throw new NoSuchElementException("해당 예약 정보를 찾을 수 없습니다.");
            }

            // 2. JSON으로 직렬화
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(reservation);

            // 3. AES 암호화 + QR 생성
            String encrypted = AesUtil.encryptWithIv(json);
            return QrCodeUtil.generateEncryptedQr(encrypted);

        } catch (Exception e) {
            log.error("예약 QR 생성 실패", e);
            throw new RuntimeException("예약 QR 생성 실패: " + e.getMessage());
        }
    }


}
