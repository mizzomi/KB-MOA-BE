package org.moa.global.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.notification.dto.NotificationResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationService {
    List<NotificationResponseDto> getNotifications();

    boolean tripNotification();

    boolean settleNotification();
}
