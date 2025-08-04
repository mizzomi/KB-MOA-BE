package org.moa.member.service;

import lombok.RequiredArgsConstructor;
import org.moa.global.service.FirebaseStorageService;
import org.moa.member.dto.idcard.*;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MyIdServiceImpl implements MyIdService {

    private final IdCardMapper idCardMapper;
    private final DriverLicenseMapper driverLicenseMapper;
    private final FirebaseStorageService firebaseStorageService;

    /** 사용자의 주민등록증 및 운전면허증 정보 조회 **/
    @Override
    public MyIdResponseDto getMyIdInfo(Long memberId) {
        // 주민등록증 정보 처리
        IdCardResponseDto idCard = processIdCard(memberId);

        // 운전면허증 정보 처리
        DriverLicenseResponseDto driverLicense = processDriverLicense(memberId);

        // 두 정보를 합쳐서 반환
        return new MyIdResponseDto(idCard, driverLicense);
    }


    /** 주민등록증 정보 처리 **/
    private IdCardResponseDto processIdCard(Long memberId) {
        IdCardRawData rawData = idCardMapper.findIdCardByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        LocalDate birthday = calculateBirthday(rawData.getIdCardNumber());

        // 파일 이름을 서명된 URL로 변환
        String signedImageUrl = getSignedUrlIfPresent(rawData.getImageUrl());

        return IdCardResponseDto.builder()
                .name(rawData.getName())
                .birthday(birthday)
                .address(rawData.getAddress())
                .imageUrl(signedImageUrl)
                .build();
    }


    /** 운전면허증 정보 처리 **/
    private DriverLicenseResponseDto processDriverLicense(Long memberId) {
        DriverLicenseRawData rawData = driverLicenseMapper.findDriverLicenseByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        LocalDate birthday = calculateBirthday(rawData.getIdCardNumber());
        String signedImageUrl = getSignedUrlIfPresent(rawData.getImageUrl());

        return DriverLicenseResponseDto.builder()
                .name(rawData.getName())
                .birthday(birthday)
                .issuingAgency(rawData.getIssuingAgency())
                .imageUrl(signedImageUrl)
                .build();
    }


    /** 이미지 파일 이름이 존재할 경우에만 서명된 URL 반환 **/
    private String getSignedUrlIfPresent(String imageFileName) {
        if (StringUtils.hasText(imageFileName)) {
            return firebaseStorageService.getSignedUrl(imageFileName);
        }
        return null;
    }


    /** 주민등록증 생년월일 구하는 메서드 **/
    private LocalDate calculateBirthday(String idCardNumber) {
        if (!StringUtils.hasText(idCardNumber) || idCardNumber.length() < 7) {
            return null;
        }
        String birthPart = idCardNumber.substring(0, 6);
        char genderDigit = idCardNumber.charAt(6);

        String yearPrefix = switch (genderDigit) {
            case '1', '2', '5', '6' -> "19"; // 1900년대생
            case '3', '4', '7', '8' -> "20"; // 2000년대생
            default -> null; // 유효하지 않은 경우
        };

        if (yearPrefix == null) {
            return null; // 유효하지 않은 주민번호 처리
        }

        return LocalDate.parse(yearPrefix + birthPart, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}