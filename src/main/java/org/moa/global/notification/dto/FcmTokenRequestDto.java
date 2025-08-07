package org.moa.global.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FcmTokenRequestDto {
    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String fcmToken;
}
