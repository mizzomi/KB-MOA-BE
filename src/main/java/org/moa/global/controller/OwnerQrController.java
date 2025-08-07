package org.moa.global.controller;

import com.google.api.Http;
import com.google.protobuf.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.service.qr.QrService;
import org.moa.global.type.StatusCode;
import org.moa.member.dto.qr.IdCardResponseDto;
import org.moa.reservation.dto.QrRestaurantReservationDto;
import org.moa.reservation.mapper.ReservationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/qr")
public class OwnerQrController {

    private final QrService qrService;
    private final ReservationMapper reservationMapper;

    // 주민등록증 QR 복호화 API
    @GetMapping(value = "/idcard")
    public ResponseEntity<ApiResponse<?>> decryptIdCardQr(@RequestParam("data") String encryptedText) {
        try {
            IdCardResponseDto response = qrService.decryptIdCardQr(encryptedText);

            if (response == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(StatusCode.NOT_FOUND, "해당하는 주민등록증 정보가 없습니다."));
            } // NOT_FOUND = 404

            return ResponseEntity.ok(ApiResponse.of(response));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "유효하지 않은 QR 코드입니다."));
        } // INTERNAL_SERVER_ERROR = 500
    }

    // 예약 내역 QR 복호화 API
    @GetMapping("/reservation")
    public ResponseEntity<ApiResponse<?>> decryptReservationQr(@RequestParam("data") String encryptedText,
                                                               @RequestParam("ownerId") Long ownerId) {
        try {
            Object response = qrService.decryptReservationQr(encryptedText, ownerId);

            if (response == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(StatusCode.NOT_FOUND, "예약 정보를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(ApiResponse.of(response));
        } catch (SecurityException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(StatusCode.FORBIDDEN, e.getMessage()));
        } catch (Exception e) {
            log.error("예약 QR 복호화 실패", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "유효하지 않은 QR 코드입니다."));
        }
    }
}

