package org.moa.global.security.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.security.dto.TokenInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token Rotation Service
 * RTR (Refresh Token Rotation) 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRotationService {

    private final JwtTokenService jwtTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Refresh Token을 사용한 Token Rotation
     */
    public TokenInfo rotateToken(String oldRefreshToken, HttpServletRequest request) {
        try {
            // 1. 기존 토큰 검증
            Claims claims = jwtTokenService.validateToken(oldRefreshToken);
            Long memberId = Long.parseLong(claims.getSubject());
            String oldFamilyId = claims.get("familyId", String.class);

            // 2. Redis에서 저장된 토큰과 비교
            String redisKey = "refresh_token:" + memberId;
            Map<Object, Object> storedTokenData = redisTemplate.opsForHash().entries(redisKey);

            if (storedTokenData.isEmpty()) {
                log.warn("No stored refresh token found for member: {}", memberId);
                throw new SecurityException("Invalid refresh token");
            }

            String storedToken = (String) storedTokenData.get("token");
            String storedFamilyId = (String) storedTokenData.get("familyId");

            // 3. 토큰 일치 여부 확인
            if (!oldRefreshToken.equals(storedToken)) {
                // 토큰 불일치 - 가능한 탈취 상황
                log.error("Token mismatch detected for member: {}. Possible token theft!", memberId);
                handlePossibleTokenTheft(memberId, oldFamilyId);
                throw new SecurityException("Invalid refresh token - possible theft detected");
            }

            // 4. Family ID 확인
            if (!oldFamilyId.equals(storedFamilyId)) {
                log.warn("Family ID mismatch for member: {}", memberId);
                throw new SecurityException("Invalid token family");
            }

            // 5. 새 토큰 생성 (같은 Family ID 유지)
            String newAccessToken = jwtTokenService.createAccessToken(memberId);
            String newRefreshToken = jwtTokenService.createRefreshToken(memberId, oldFamilyId);

            // 6. 기존 Refresh Token을 블랙리스트에 추가
            jwtTokenService.addToBlacklist(oldRefreshToken, "refresh");

            // 7. Redis 업데이트
            updateRefreshTokenInRedis(memberId, newRefreshToken, oldFamilyId, request);

            // 8. 새 토큰 정보 반환
            return TokenInfo.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .familyId(oldFamilyId)
                    .accessTokenExpiresIn(15 * 60 * 1000L) // 15분
                    .refreshTokenExpiresIn(7 * 24 * 60 * 60 * 1000L) // 7일
                    .build();

        } catch (Exception e) {
            log.error("Token rotation failed: {}", e.getMessage());
            throw new SecurityException("Token rotation failed", e);
        }
    }

    /**
     * Redis에서 Refresh Token 업데이트
     */
    private void updateRefreshTokenInRedis(Long memberId, String newToken, String familyId, 
                                          HttpServletRequest request) {
        String redisKey = "refresh_token:" + memberId;
        
        // 기존 데이터 삭제
        redisTemplate.delete(redisKey);
        
        // 새 데이터 저장
        Map<String, Object> tokenData = Map.of(
            "token", newToken,
            "familyId", familyId,
            "issuedAt", System.currentTimeMillis(),
            "expiresAt", System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000,
            "userAgent", request != null ? request.getHeader("User-Agent") : "",
            "ipAddress", request != null ? getClientIp(request) : ""
        );
        
        redisTemplate.opsForHash().putAll(redisKey, tokenData);
        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
    }

    /**
     * 토큰 탈취 가능성 감지 시 처리
     */
    private void handlePossibleTokenTheft(Long memberId, String familyId) {
        // 1. 해당 사용자의 모든 Refresh Token 무효화
        String refreshTokenKey = "refresh_token:" + memberId;
        redisTemplate.delete(refreshTokenKey);

        // 2. Family ID 삭제
        if (familyId != null) {
            String familyKey = "family:" + familyId;
            redisTemplate.delete(familyKey);
        }

        // 3. 알림 또는 로그 (실제 환경에서는 알림 시스템 연동)
        log.error("SECURITY ALERT: Possible token theft detected for member: {}. All tokens invalidated.", memberId);
        
        // TODO: 사용자에게 이메일/SMS 알림 전송
        // TODO: 보안 이벤트 로그 DB 저장
    }

    /**
     * 로그아웃 처리
     */
    public void logout(Long memberId, String accessToken, String refreshToken) {
        try {
            // 1. Access Token 블랙리스트 등록
            if (accessToken != null && !accessToken.isEmpty()) {
                jwtTokenService.addToBlacklist(accessToken, "access");
            }

            // 2. Refresh Token 블랙리스트 등록
            if (refreshToken != null && !refreshToken.isEmpty()) {
                jwtTokenService.addToBlacklist(refreshToken, "refresh");
                
                // 3. Redis에서 Refresh Token 삭제
                String redisKey = "refresh_token:" + memberId;
                redisTemplate.delete(redisKey);
                
                // 4. Family ID 삭제
                try {
                    Claims claims = jwtTokenService.parseToken(refreshToken);
                    String familyId = claims.get("familyId", String.class);
                    if (familyId != null) {
                        redisTemplate.delete("family:" + familyId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract family ID from refresh token: {}", e.getMessage());
                }
            }

            log.info("User {} logged out successfully", memberId);
        } catch (Exception e) {
            log.error("Logout failed for member {}: {}", memberId, e.getMessage());
        }
    }

    /**
     * Client IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 특정 사용자의 모든 토큰 무효화 (보안 목적)
     */
    public void invalidateAllTokens(Long memberId) {
        try {
            // Redis에서 Refresh Token 정보 가져오기
            String redisKey = "refresh_token:" + memberId;
            Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(redisKey);
            
            if (!tokenData.isEmpty()) {
                String familyId = (String) tokenData.get("familyId");
                
                // Family ID 삭제
                if (familyId != null) {
                    redisTemplate.delete("family:" + familyId);
                }
            }
            
            // Refresh Token 삭제
            redisTemplate.delete(redisKey);
            
            log.info("All tokens invalidated for member: {}", memberId);
        } catch (Exception e) {
            log.error("Failed to invalidate all tokens for member {}: {}", memberId, e.getMessage());
        }
    }
}
