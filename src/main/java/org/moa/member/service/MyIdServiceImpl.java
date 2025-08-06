package org.moa.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.service.FirebaseStorageService;
import org.moa.member.dto.idcard.*;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyIdServiceImpl implements MyIdService {

    private final IdCardMapper idCardMapper;
    private final DriverLicenseMapper driverLicenseMapper;
    private final FirebaseStorageService firebaseStorageService;

    /** 사용자의 주민등록증 및 운전면허증 정보 조회 **/
    @Override
    public MyIdResponseDto getMyIdInfo(Long memberId) {
        // [로그 추가] 전체 작업 시작 시간 기록
        long totalStartTime = System.currentTimeMillis();

        CompletableFuture<IdCardResponseDto> idCardFuture = CompletableFuture.supplyAsync(() -> processIdCard(memberId));
        CompletableFuture<DriverLicenseResponseDto> driverLicenseFuture = CompletableFuture.supplyAsync(() -> processDriverLicense(memberId));

        // thenCombine: 두 Future가 모두 성공적으로 완료되면, 그 결과들을 조합하여 새로운 결과를 만듦.
        MyIdResponseDto result = idCardFuture.thenCombine(driverLicenseFuture, MyIdResponseDto::new)
                .exceptionally(ex -> {
                    // exceptionally: 비동기 처리 중 예외가 발생했을 때 실행
                    log.error("ID 정보 비동기 조회 중 오류 발생", ex);
                    // 비동기 파이프라인에서 발생한 예외를 래핑하여 다시 던짐
                    throw new RuntimeException("ID 정보 조회 중 오류가 발생했습니다.", ex);
                })
                .join(); // 모든 작업이 완료될 때까지 현재 스레드를 블로킹하고 최종 결과를 반환합니다.

        // [로그 추가] 전체 작업 종료 시간 기록 및 소요 시간 출력
        long totalEndTime = System.currentTimeMillis();
        log.info("getMyIdInfo - 총 소요 시간: {}ms", (totalEndTime - totalStartTime));

        return result;
    }


    /** 주민등록증 정보 처리 **/
    private IdCardResponseDto processIdCard(Long memberId) {
        long startTime = System.currentTimeMillis(); // [로그 추가]
        IdCardRawData rawData = idCardMapper.findIdCardByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        // 이 부분은 CPU 연산이므로 비동기 처리의 이점이 크지 않지만, getMyIdInfo에서 구조상 분리
        LocalDate birthday = calculateBirthday(rawData.getIdCardNumber());

        // 파일 이름을 서명된 URL로 변환
        String signedImageUrl = getSignedUrl(rawData.getImageUrl());

        IdCardResponseDto response = IdCardResponseDto.builder()
                .name(rawData.getName())
                .birthday(birthday)
                .address(rawData.getAddress())
                .imageUrl(signedImageUrl)
                .idCardNumber(rawData.getIdCardNumber())
                .issuedDate(rawData.getIssuedDate())
                .build();

        long endTime = System.currentTimeMillis(); // [로그 추가]
        log.info("processIdCard - 소요 시간: {}ms", (endTime - startTime)); // [로그 추가]
        return response;
    }


    /** 운전면허증 정보 처리 **/
    private DriverLicenseResponseDto processDriverLicense(Long memberId) {
        long startTime = System.currentTimeMillis(); // [로그 추가]
        DriverLicenseRawData rawData = driverLicenseMapper.findDriverLicenseByMemberId(memberId);
        if (rawData == null) {
            return null;
        }

        String signedImageUrl = getSignedUrl(rawData.getImageUrl());

        DriverLicenseResponseDto response = DriverLicenseResponseDto.builder()
                .name(rawData.getName())
                .licenseNumber(rawData.getLicenseNumber())
                .idCardNumber(rawData.getIdCardNumber())
                .licenseType(rawData.getLicenseType())
                .issuingAgency(rawData.getIssuingAgency())
                .issuedDate(rawData.getIssuedDate())
                .expiryDate(rawData.getExpiryDate())
                .imageUrl(signedImageUrl)
                .build();

        long endTime = System.currentTimeMillis(); // [로그 추가]
        log.info("processDriverLicense - 소요 시간: {}ms", (endTime - startTime)); // [로그 추가]
        return response;
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