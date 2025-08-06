package org.moa.member.service;

import lombok.RequiredArgsConstructor;
import org.moa.global.service.FirebaseStorageService;
import org.moa.member.dto.idcard.*;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
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
        // [수정] 주민등록증과 운전면허증 정보를 동시에 조회 시작
        CompletableFuture<IdCardResponseDto> idCardFuture = CompletableFuture.supplyAsync(() -> processIdCard(memberId));
        CompletableFuture<DriverLicenseResponseDto> driverLicenseFuture = CompletableFuture.supplyAsync(() -> processDriverLicense(memberId));

        // [수정] 두 작업이 모두 완료될 때까지 대기
        CompletableFuture.allOf(idCardFuture, driverLicenseFuture).join();

        try {
            // [수정] 완료된 Future에서 결과 가져오기
            IdCardResponseDto idCard = idCardFuture.get();
            DriverLicenseResponseDto driverLicense = driverLicenseFuture.get();

            // 두 정보를 합쳐서 반환
            return new MyIdResponseDto(idCard, driverLicense);
        } catch (Exception e) {
            // InterruptedException, ExecutionException 처리
            // 실제 프로덕션 코드에서는 더 정교한 예외 처리가 필요할 수 있음
            Thread.currentThread().interrupt(); // InterruptedException 발생 시 스레드 인터럽트 상태 복원
            throw new RuntimeException("ID 정보 조회 중 비동기 처리 오류 발생", e);
        }
    }


    /** 주민등록증 정보 처리 **/
    private IdCardResponseDto processIdCard(Long memberId) {
        IdCardRawData rawData = idCardMapper.findIdCardByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        // [수정] 이 부분은 CPU 연산이므로 비동기 처리의 이점이 크지 않지만, getMyIdInfo에서 구조상 분리
        LocalDate birthday = calculateBirthday(rawData.getIdCardNumber());

        // 파일 이름을 서명된 URL로 변환
        String signedImageUrl = getSignedUrl(rawData.getImageUrl());

        return IdCardResponseDto.builder()
                .name(rawData.getName())
                .birthday(birthday)
                .address(rawData.getAddress())
                .imageUrl(signedImageUrl)
                .idCardNumber(rawData.getIdCardNumber())
                .issuedDate(rawData.getIssuedDate())
                .build();
    }


    /** 운전면허증 정보 처리 **/
    private DriverLicenseResponseDto processDriverLicense(Long memberId) {
        DriverLicenseRawData rawData = driverLicenseMapper.findDriverLicenseByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        String signedImageUrl = getSignedUrl(rawData.getImageUrl());

        return DriverLicenseResponseDto.builder()
                .name(rawData.getName())
                .licenseNumber(rawData.getLicenseNumber())
                .idCardNumber(rawData.getIdCardNumber())
                .licenseType(rawData.getLicenseType())
                .issuingAgency(rawData.getIssuingAgency())
                .issuedDate(rawData.getIssuedDate())
                .expiryDate(rawData.getExpiryDate())
                .imageUrl(signedImageUrl)
                .build();
    }


    /** 이미지 파일 이름이 존재할 경우에만 서명된 URL 반환 **/
    private String getSignedUrl(String imageFileName) {
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