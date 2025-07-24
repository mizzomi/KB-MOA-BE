package org.moa.global.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.service.qr.QrService;
import org.moa.global.type.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberQrController {

    private final QrService qrService;

    // 사용자 주민등록증 QR 생성
    @GetMapping("/qr-idcard")
    public ResponseEntity<ApiResponse<?>> generateIdCardQr(@RequestParam("memberId") Long memberId) {
        try {
            String base64Qr = qrService.generateIdCardQr(memberId);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.of(base64Qr)); // OK = 200

        } catch (Exception e) {
            log.error("QR 생성 실패", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // INTERNAL_SERVER_ERROR = 500
                    .body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "QR 생성 실패: " + e.getMessage()));
        }
    }
}
