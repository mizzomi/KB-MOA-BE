package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;  // 만료 시간 (밀리초)
    private Long refreshTokenExpiresIn; // 만료 시간 (밀리초)
    private String tokenType = "Bearer";
}
