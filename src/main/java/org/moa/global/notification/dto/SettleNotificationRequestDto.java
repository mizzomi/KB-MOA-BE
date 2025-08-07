package org.moa.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettleNotificationRequestDto {
    private Long tripId;
    private Long notificationId;
}
