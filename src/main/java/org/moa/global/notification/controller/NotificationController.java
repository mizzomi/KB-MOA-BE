package org.moa.global.notification.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.notification.dto.FcmTokenRequestDto;
import org.moa.global.notification.dto.SettleNotificationRequestDto;
import org.moa.global.notification.dto.TripNotificationRequestDto;
import org.moa.global.notification.service.NotificationService;
import org.moa.global.response.ApiResponse;
import org.moa.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final MemberService memberService;

    // FCM 토큰 업데이트/삭제
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<?>> updateFcmToken(@RequestBody FcmTokenRequestDto dto) {
        // FCM 토큰 저장
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.updateFcmToken(dto)));
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<?>> deleteFcmToken() {
        // FCM 토큰 삭제
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.deleteFcmToken()));
    }

    @GetMapping("/notification")
    public ResponseEntity<ApiResponse<?>> getNotifications() {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.getNotifications()));
    }

    @PostMapping("/trip-notification")
    public ResponseEntity<ApiResponse<?>> tripNotification(@RequestBody TripNotificationRequestDto dto){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.tripNotification(dto)));
    }

    @PostMapping("/settle-notification")
    public ResponseEntity<ApiResponse<?>> settleNotification(@RequestBody SettleNotificationRequestDto dto){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.settleNotification(dto)));
    }
}
