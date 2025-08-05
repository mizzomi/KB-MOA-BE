package org.moa.global.notification.entity;

import lombok.*;
import org.moa.global.type.NotificationType;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long notificationId;
    private Long memberId;
    private NotificationType notificationType;
    private String tripName;
    private String title;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
}
