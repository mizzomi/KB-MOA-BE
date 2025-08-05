package org.moa.global.notification.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.notification.dto.NotificationResponseDto;
import org.moa.global.notification.service.NotificationService;
import org.moa.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/notification")
    public ResponseEntity<ApiResponse<?>> getNotifications() {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.getNotifications()));
    }

    @PostMapping("/trip-notification")
    public ResponseEntity<ApiResponse<?>> tripNotification(){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.tripNotification()));
    }

    @PostMapping("/settle-notification")
    public ResponseEntity<ApiResponse<?>> settleNotification(){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(notificationService.settleNotification()));
    }
}
