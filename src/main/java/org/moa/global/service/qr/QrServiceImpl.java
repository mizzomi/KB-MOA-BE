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
    public String generateIdCardQr(Long memberId) throws Exception {
            // ========= 테스트용 =========
            // memberId가 1이면 고정된 QR Base64 반환
            if (memberId == 1) {
                Map<String, Long> info = new HashMap<>();
                info.put("member_id", memberId);
                String json = toJson(info);

                String encrypted = AesUtil.encryptWithIv(json);

                log.info("Postman 테스트용 data 파라미터: {}", encrypted);

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

            String json = toJson(info); // json = "{\"member_id\":1}" 로 직렬화

            return QrCodeUtil.generateEncryptedQr(json);
    }

    // 주민등록증 QR 복호화 API
    @Override
    public IdCardResponseDto decryptIdCardQr(String encryptedText) {
            String json = AesUtil.decryptWithIv(encryptedText);
            Map<String, Object> data = fromJson(json); // json -> map 으로 파싱

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
    }

    // 예약 내역 QR 생성 API
    @Override
    public String generateReservationQr(Long reservationId, Long memberId) throws Exception {
        // 1. 권한 검사
        boolean isMember = reservationMapper.isTripMemberByReservationIdAndMemberId(reservationId, memberId);

        if (!isMember) {
            throw new SecurityException("이 예약에 접근할 권한이 없습니다.");
        }

        // 2. 타입 조회
        String type = reservationMapper.findTypeByReservationId(reservationId);

        if (type == null) {
            throw new NoSuchElementException("해당 예약 정보를 찾을 수 없습니다.");
        }

        // 3. 식당 / 숙박 / 교통
        Object reservationDto = switch (type) {
            case "RESTAURANT" -> reservationMapper.findRestQrInfoByReservationId(reservationId);
            case "ACCOMMODATION" -> reservationMapper.findAccomQrInfoByReservationId(reservationId);
            case "TRANSPORT" -> reservationMapper.findTransQrInfoByReservationId(reservationId);
            default -> throw new IllegalArgumentException("지원하지 않는 예약 타입입니다: " + type);
        };

        if (reservationDto == null) {
            throw new NoSuchElementException("해당 예약 정보를 찾을 수 없습니다.");
        }

        // 4. JSON 직렬화
        String json = toJson(reservationDto);

        // 5. 암호화 및 QR 생성
        String encrypted = AesUtil.encryptWithIv(json);
        return QrCodeUtil.generateEncryptedQr(encrypted);
    }

    // 직렬화 메서드
    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 직렬화 실패: {}", obj, e);
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON 역직렬화 실패: {}", json, e);
            throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }
}
