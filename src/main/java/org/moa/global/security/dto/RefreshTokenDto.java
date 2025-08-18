package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Redis에 저장될 Refresh Token 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDto {
    
    private String token;
    private String familyId;
    private String userId;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String userAgent;       // 브라우저 정보
    private String ipAddress;       // IP 주소 (선택적)
}
