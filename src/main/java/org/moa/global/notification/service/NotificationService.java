package org.moa.global.notification.service;

import org.moa.global.notification.dto.NotificationResponseDto;
import org.moa.global.notification.dto.SettleNotificationRequestDto;
import org.moa.global.notification.dto.TripNotificationRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationService {
    List<NotificationResponseDto> getNotifications();

    boolean tripNotification(TripNotificationRequestDto dto);

    boolean settleNotification(SettleNotificationRequestDto dto);
}
