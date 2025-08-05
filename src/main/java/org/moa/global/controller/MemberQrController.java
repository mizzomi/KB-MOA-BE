package org.moa.global.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.service.qr.QrService;
import org.moa.global.type.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/qr")
public class MemberQrController {

    private final QrService qrService;

    // 사용자 주민등록증 QR 생성
    @GetMapping("/idcard")
    public ResponseEntity<ApiResponse<?>> generateIdCardQr(@AuthenticationPrincipal CustomUser user) {
        try {
            Long memberId = user.getMember().getMemberId();
            String base64Qr = qrService.generateIdCardQr(memberId);

            if (base64Qr == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(StatusCode.NOT_FOUND, "해당 회원의 주민등록증 정보가 존재하지 않습니다."));
            }

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.of(base64Qr)); // OK = 200

        } catch (NoSuchElementException e) {
            log.warn("회원 정보 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(StatusCode.NOT_FOUND, e.getMessage())); // NOT_FOUND = 404

        } catch (Exception e) {
            log.error("QR 생성 실패", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // INTERNAL_SERVER_ERROR = 500
                    .body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "QR 코드 생성 중 오류가 발생했습니다. 다시 시도해주세요."));
        }
    }

}
