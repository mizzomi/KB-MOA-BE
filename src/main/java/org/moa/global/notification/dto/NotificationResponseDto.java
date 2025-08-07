package org.moa.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moa.global.type.NotificationType;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class NotificationResponseDto {
    private Long notificationId;
    private Long tripId;
    private Long expenseId;
    private NotificationType notificationType;
    private String sender;
    private String tripName;
}
