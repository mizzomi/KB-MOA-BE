package org.moa.global.service.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.util.AesUtil;
import org.moa.global.util.QrCodeUtil;
import org.moa.member.dto.qr.IdCardResponseDto;
import org.moa.member.entity.IdCard;
import org.moa.member.mapper.IdCardMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService{

    private final IdCardMapper idCardMapper; // myBatis 매퍼 주입, DB에서 주민등록증 조회할 때 사용

    // 주민등록증 QR 생성 API
    @Override
    public String generateIdCardQr(Long memberId) {
        try {
            Map<String, Long> info = new HashMap<>();
            info.put("member_id", memberId); // QR 안에 들어갈 단일 값 : member_id

            String json = new ObjectMapper().writeValueAsString(info); // json = "{\"member_id\":1}" 로 직렬화

            return QrCodeUtil.generateEncryptedQr(json);

        } catch (Exception e) {
            log.error("QR 생성 실패", e);
            throw new RuntimeException("QR 생성 실패: " + e.getMessage()); // error를 Controller로 던짐
        }
    }

    // 주민등록증 QR 복호화 API
    @Override
    public IdCardResponseDto decryptIdCardQr(String encryptedText) {
        try {
            String json = AesUtil.decryptWithIv(encryptedText); // AesUtil.decryptWithIv 사용
            Map<String, Object> data = new ObjectMapper().readValue(json, Map.class); // json -> map 으로 파싱

            Long memberId = Long.valueOf(data.get("member_id").toString());
            IdCard card = idCardMapper.findByMemberId(memberId); // memberId를 통해 DB 조회해서 정보 가져옴

            if (card == null) {
                return null;
            } // Controller에서 404 응답 처리

            return new IdCardResponseDto(
                    card.getName(),
                    card.getIdCardNumber(),
                    card.getIssuedDate().toString(),
                    card.getAddress(),
                    card.getImageUrl()
            ); // DTO
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패 : " + e.getMessage());
        } // 복호화/파싱 실패 시 500 error
    }
}
