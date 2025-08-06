package org.moa.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripNotificationRequestDto {
    private String type; // "수락" , "거절"
    private Long tripId;
    private Long notificationId;
}
