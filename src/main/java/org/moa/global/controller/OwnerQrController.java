package org.moa.global.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.service.qr.QrService;
import org.moa.global.type.StatusCode;
import org.moa.member.dto.qr.IdCardResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner")
public class OwnerQrController {

    private final QrService qrService;

    // 주민등록증 QR 복호화 API
    @GetMapping(value = "/qr-idcard")
    public ResponseEntity<ApiResponse<?>> decryptQrData(@RequestParam("data") String encryptedText) {
        try {
            IdCardResponseDto response = qrService.decryptIdCardQr(encryptedText);

            if (response == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(StatusCode.NOT_FOUND, "해당하는 주민등록증 정보가 없습니다."));
            } // NOT_FOUND = 404

            return ResponseEntity.ok(ApiResponse.of(response)); // OK로 DTO 반환

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "복호화 실패: " + e.getMessage()));
        } // INTERNAL_SERVER_ERROR = 500
    }
}

