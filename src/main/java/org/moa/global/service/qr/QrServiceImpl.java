package org.moa.global.service.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.service.FirebaseStorageService;
import org.moa.global.util.AesUtil;
import org.moa.global.util.QrCodeUtil;
import org.moa.member.dto.qr.IdCardResponseDto;
import org.moa.member.entity.IdCard;
import org.moa.member.mapper.IdCardMapper;
import org.moa.reservation.dto.qr.*;
import org.moa.reservation.mapper.ReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService{

    private final IdCardMapper idCardMapper;
    private final ReservationMapper reservationMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final ObjectMapper objectMapper;

    // 주민등록증 QR 생성 API
    @Override
    public String generateIdCardQr(Long memberId) throws Exception {

        // 1. DB에서 해당 memberId의 주민등록증 정보 존재 확인
        IdCard card = idCardMapper.findByMemberId(memberId);
        if(card == null) {
            throw new NoSuchElementException("해당 회원의 주민등록증 정보가 없습니다.");
        }

        // 2. QR 생성
        Map<String, Long> info = new HashMap<>();
        info.put("member_id", memberId); // QR 안에 들어갈 단일 값 : member_id

        String json = toJson(info); // json = "{\"member_id\":1}" 로 직렬화

        String encrypted = AesUtil.encryptWithIv(json);
        log.info("Postman 테스트용 주민등록증 QR data 파라미터: {}", encrypted);

        return QrCodeUtil.generateEncryptedQr(encrypted);
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

        String signedImageUrl = null;
        if (StringUtils.hasText(card.getImageUrl())) {
            // DB에서 가져온 파일 이름을 서명된 URL로 변환
            signedImageUrl = firebaseStorageService.getSignedUrl(card.getImageUrl());
        }

        return new IdCardResponseDto(
                card.getName(),
                card.getIdCardNumber(),
                card.getIssuedDate().toString(),
                card.getAddress(),
                signedImageUrl
        );
    }

    // 예약 내역 QR 생성 API
    @Override
    public Object generateReservationQr(Long reservationId, Long memberId) throws Exception {

        // 1. 권한 검사
        boolean isMember = reservationMapper.isTripMemberByReservationIdAndMemberId(reservationId, memberId);

        if (!isMember) {
            throw new SecurityException("이 예약에 접근할 권한이 없습니다.");
        }

        // 2. 예약 타입 조회
        String type = reservationMapper.findTypeByReservationId(reservationId);

        if (type == null) {
            throw new NoSuchElementException("해당 예약 정보를 찾을 수 없습니다.");
        }

        // 3. 예약 타입에 따라 분기
        switch (type) {
            case "TRANSPORT" : {
                List<UserTransportReservationDto> trans = reservationMapper.findUserTransInfoByReservationId(reservationId);
                if (trans == null || trans.isEmpty()) {
                    throw new NoSuchElementException("해당 예약의 교통 정보를 찾을 수 없습니다.");
                }

                List<QrItemResponseDto<UserTransportReservationDto>> responseList = new ArrayList<>();
                for (UserTransportReservationDto tran : trans) {
                    Map<String, Long> info = Collections.singletonMap("tran_res_id", tran.getTranResId());
                    String json = toJson(info);
                    String encrypted = AesUtil.encryptWithIv(json);
                    String qrCodeString = QrCodeUtil.generateEncryptedQr(encrypted);
                    responseList.add(new QrItemResponseDto<>(qrCodeString, tran));
                }

                return responseList;
            }

            case "ACCOMMODATION" : {
                List<UserAccommodationReservationDto> accoms = reservationMapper.findUserAccomInfoByReservationId(reservationId);
                if (accoms == null || accoms.isEmpty()) {
                    throw new NoSuchElementException("해당 예약의 숙소 정보를 찾을 수 없습니다.");
                }

                List<QrItemResponseDto<UserAccommodationReservationDto>> responseList = new ArrayList<>();
                for (UserAccommodationReservationDto accom : accoms) {
                    Map<String, Long> info = Collections.singletonMap("accom_res_id", accom.getAccomResId());
                    String json = toJson(info);
                    String encrypted = AesUtil.encryptWithIv(json);
                    String qrCodeString = QrCodeUtil.generateEncryptedQr(encrypted);
                    responseList.add(new QrItemResponseDto<>(qrCodeString, accom));
                }

                return responseList;
            }

            case "RESTAURANT" : {
                List<UserRestaurantReservationDto> rests = reservationMapper.findUserRestInfoByReservationId(reservationId);
                if (rests == null || rests.isEmpty()) {
                    throw new NoSuchElementException("해당 예약의 숙소 정보를 찾을 수 없습니다.");
                }

                List<QrItemResponseDto<UserRestaurantReservationDto>> responseList = new ArrayList<>();
                for (UserRestaurantReservationDto rest : rests) {
                    Map<String, Long> info = Collections.singletonMap("rest_res_id", rest.getRestResId());
                    String json = toJson(info);
                    String encrypted = AesUtil.encryptWithIv(json);
                    String qrCodeString = QrCodeUtil.generateEncryptedQr(encrypted);
                    responseList.add(new QrItemResponseDto<>(qrCodeString, rest));
                }

                return responseList;
            }

            default :
                throw new IllegalArgumentException("지원하지 않는 예약 타입입니다." + type);
        }
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

    // 예약 상세 정보 조회 API
    @Override
    public Object getReservationInfo(Long reservationId, Long memberId) {

        // 1. 예약 권한이 있는지 확인
        boolean isMember = reservationMapper.isTripMemberByReservationIdAndMemberId(reservationId, memberId);
        if (!isMember) {
            throw new SecurityException("이 예약 정보를 조회할 권한이 없습니다.");
        }

        // 2. 예약 타입 조회
        String type = reservationMapper.findTypeByReservationId(reservationId);
        if (type == null) {
            throw new NoSuchElementException("해당 예약 정보를 찾을 수 없습니다.");
        }

        // 3. 타입에 맞는 예약 정보를 DB에서 가져와 반환
        return switch (type) {
            case "RESTAURANT" -> reservationMapper.findUserRestInfoByReservationId(reservationId);
            case "ACCOMMODATION" -> reservationMapper.findUserAccomInfoByReservationId(reservationId);
            case "TRANSPORT" -> reservationMapper.findUserTransInfoByReservationId(reservationId);
            default -> throw new IllegalArgumentException("지원하지 않는 예약 타입입니다: " + type);
        };
    }

    // 예약 내역 QR 복호화 API
    @Override
    public Object decryptReservationQr(String encryptedText, Long ownerId) {

        String json = AesUtil.decryptWithIv(encryptedText);
        Map<String, Object> data = fromJson(json);

        if (data.containsKey("tran_res_id")) {
            Long tranResId = Long.valueOf(data.get("tran_res_id").toString());

            UserTransportReservationDto dto = reservationMapper.findTransInfoByTranResId(tranResId);
            if (dto == null) throw new NoSuchElementException("교통 예약 좌석 정보를 찾을 수 없습니다.");

            if (!reservationMapper.isOwnerOfBusiness(ownerId, dto.getTransportId(),"TRANSPORT")) {
                throw new SecurityException("이 예약에 접근할 권한이 없습니다.");
            }

            return dto;
        } else if (data.containsKey("rest_res_id")) {
            Long restResId = Long.valueOf(data.get("rest_res_id").toString());

            UserRestaurantReservationDto dto = reservationMapper.findRestInfoByRestResId(restResId);
            if (dto == null) throw new NoSuchElementException("식당 예약 정보를 찾을 수 없습니다.");

            if (!reservationMapper.isOwnerOfBusiness(ownerId, dto.getRestId(),"RESTAURANT")) {
                throw new SecurityException("이 예약에 접근할 권한이 없습니다.");
            }

            return dto;
        } else if (data.containsKey("accom_res_id")) {
            Long accomResId = Long.valueOf(data.get("accom_res_id").toString());

            UserAccommodationReservationDto dto = reservationMapper.findAccomInfoByAccomResId(accomResId);
            if (dto == null) throw new NoSuchElementException("숙소 예약 정보를 찾을 수 없습니다.");

            if (!reservationMapper.isOwnerOfBusiness(ownerId, dto.getAccomId(),"ACCOMMODATION")) {
                throw new SecurityException("이 예약에 접근할 권한이 없습니다.");
            }

            return dto;
        }
        throw new IllegalArgumentException("유효하지 않은 QR 데이터입니다.");
    }
}
