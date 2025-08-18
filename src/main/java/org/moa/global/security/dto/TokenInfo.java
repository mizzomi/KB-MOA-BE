package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT Token 정보를 담는 DTO
 * RTR (Refresh Token Rotation) 구현을 위한 Token 정보 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {
    
    private String accessToken;
    private String refreshToken;
    private String familyId;       // RTR을 위한 Token Family ID
    private Long accessTokenExpiresIn;  // Access Token 만료 시간 (밀리초)
    private Long refreshTokenExpiresIn; // Refresh Token 만료 시간 (밀리초)
    @Builder.Default
    private String tokenType = "Bearer";
}
