package org.moa.global.security.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.moa.global.security.domain.RefreshToken;
import org.moa.global.security.exception.TokenRefreshException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    
    @Value("${jwt.access.expiration:15}")
    private int accessTokenExpirationMinutes;
    
    @Value("${jwt.refresh.expiration:7}")
    private int refreshTokenExpirationDays;
    
    // Redis Key Prefixes
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String TOKEN_FAMILY_PREFIX = "token_family:";
    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_BLACKLIST_PREFIX = "blacklist:refresh:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final String TOKEN_METADATA_PREFIX = "token_meta:";
    
    /**
     * Refresh Token 생성 및 Redis 저장 (최초 로그인)
     */
    public RefreshToken createRefreshToken(Long memberId) {
        String tokenValue = UUID.randomUUID().toString();
        String tokenFamily = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .memberId(memberId)
                .token(tokenValue)
                .tokenFamily(tokenFamily)
                .expiryDate(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        
        // 1. Refresh Token 저장 (key: token값, value: RefreshToken 객체)
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenValue;
        redisTemplate.opsForValue().set(
            tokenKey, 
            refreshToken, 
            refreshTokenExpirationDays, 
            TimeUnit.DAYS
        );
        
        // 2. Token Family 매핑 (key: family ID, value: token set)
        String familyKey = TOKEN_FAMILY_PREFIX + tokenFamily;
        stringRedisTemplate.opsForSet().add(familyKey, tokenValue);
        stringRedisTemplate.expire(familyKey, refreshTokenExpirationDays, TimeUnit.DAYS);
        
        // 3. User Token 목록 (key: user ID, value: token set)
        String userKey = USER_TOKENS_PREFIX + memberId;
        stringRedisTemplate.opsForSet().add(userKey, tokenValue);
        stringRedisTemplate.expire(userKey, refreshTokenExpirationDays, TimeUnit.DAYS);
        
        // 4. Token 메타데이터 저장 (추적용)
        saveTokenMetadata(tokenValue, memberId, tokenFamily, "LOGIN");
        
        log.info("✅ Refresh Token created - memberId: {}, family: {}", memberId, tokenFamily);
        return refreshToken;
    }
    
    /**
     * Refresh Token Rotation (RTR)
     */
    public RefreshToken rotateRefreshToken(String oldToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + oldToken;
        RefreshToken existingToken = (RefreshToken) redisTemplate.opsForValue().get(tokenKey);
        
        // 1. 토큰이 존재하지 않음
        if (existingToken == null) {
            // 블랙리스트 확인 (재사용 시도 감지)
            if (isRefreshTokenBlacklisted(oldToken)) {
                log.error("🚨 SECURITY ALERT: Blacklisted token reuse attempt!");
                handleTokenTheft(oldToken);
                throw new TokenRefreshException("Token has been revoked - possible theft detected");
            }
            throw new TokenRefreshException("Invalid or expired refresh token");
        }
        
        // 2. 만료된 토큰
        if (existingToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            deleteRefreshToken(oldToken);
            throw new TokenRefreshException("Refresh token has expired");
        }
        
        // 3. 정상 Rotation 처리
        String tokenFamily = existingToken.getTokenFamily();
        Long memberId = existingToken.getMemberId();
        
        // 기존 토큰 블랙리스트 추가
        blacklistRefreshToken(oldToken, "ROTATED");
        
        // 기존 토큰 삭제
        deleteRefreshToken(oldToken);
        
        // 새 토큰 생성
        String newTokenValue = UUID.randomUUID().toString();
        RefreshToken newToken = RefreshToken.builder()
                .memberId(memberId)
                .token(newTokenValue)
                .tokenFamily(tokenFamily)  // 같은 Family 유지!
                .expiryDate(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        
        // 새 토큰 저장
        String newTokenKey = REFRESH_TOKEN_PREFIX + newTokenValue;
        redisTemplate.opsForValue().set(
            newTokenKey, 
            newToken, 
            refreshTokenExpirationDays, 
            TimeUnit.DAYS
        );
        
        // Family 업데이트
        String familyKey = TOKEN_FAMILY_PREFIX + tokenFamily;
        stringRedisTemplate.opsForSet().remove(familyKey, oldToken);
        stringRedisTemplate.opsForSet().add(familyKey, newTokenValue);
        
        // User Token 목록 업데이트
        String userKey = USER_TOKENS_PREFIX + memberId;
        stringRedisTemplate.opsForSet().remove(userKey, oldToken);
        stringRedisTemplate.opsForSet().add(userKey, newTokenValue);
        
        // 메타데이터 저장
        saveTokenMetadata(newTokenValue, memberId, tokenFamily, "ROTATED_FROM:" + oldToken.substring(0, 8));
        
        log.info("🔄 Token rotated - old: {}..., new: {}...", 
                oldToken.substring(0, 8), newTokenValue.substring(0, 8));
        
        return newToken;
    }
    
    /**
     * Access Token 블랙리스트 추가 (강제 로그아웃, 권한 변경 등)
     * @param token JWT token string
     * @param remainingTTL 남은 유효시간 (밀리초)
     * @param reason 블랙리스트 사유
     */
    public void blacklistAccessToken(String token, long remainingTTL, String reason) {
        if (remainingTTL <= 0) {
            return; // 이미 만료된 토큰은 블랙리스트 불필요
        }
        
        String blacklistKey = ACCESS_BLACKLIST_PREFIX + token;
        
        // 남은 TTL 시간만큼만 블랙리스트에 보관
        stringRedisTemplate.opsForValue().set(
            blacklistKey, 
            reason, 
            remainingTTL, 
            TimeUnit.MILLISECONDS
        );
        
        log.info("⛔ Access Token blacklisted - reason: {}, TTL: {}s", 
                reason, remainingTTL / 1000);
    }
    
    /**
     * Access Token이 블랙리스트에 있는지 확인
     */
    public boolean isAccessTokenBlacklisted(String token) {
        String blacklistKey = ACCESS_BLACKLIST_PREFIX + token;
        Boolean exists = stringRedisTemplate.hasKey(blacklistKey);
        
        if (Boolean.TRUE.equals(exists)) {
            String reason = stringRedisTemplate.opsForValue().get(blacklistKey);
            log.debug("Blacklisted token detected - reason: {}", reason);
            return true;
        }
        return false;
    }
    
    /**
     * Refresh Token 블랙리스트 추가
     */
    private void blacklistRefreshToken(String token, String reason) {
        String blacklistKey = REFRESH_BLACKLIST_PREFIX + token;
        
        // Refresh Token 블랙리스트는 더 오래 보관 (감사 목적)
        stringRedisTemplate.opsForValue().set(
            blacklistKey, 
            reason, 
            refreshTokenExpirationDays + 7,  // 원래 만료일 + 7일
            TimeUnit.DAYS
        );
    }
    
    /**
     * Refresh Token이 블랙리스트에 있는지 확인
     */
    private boolean isRefreshTokenBlacklisted(String token) {
        String blacklistKey = REFRESH_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey));
    }
    
    /**
     * 토큰 탈취 처리 - Family 전체 무효화
     */
    private void handleTokenTheft(String suspiciousToken) {
        // 메타데이터에서 Family 정보 조회
        String metaKey = TOKEN_METADATA_PREFIX + suspiciousToken.substring(0, 8);
        String metadata = stringRedisTemplate.opsForValue().get(metaKey);
        
        if (metadata != null && metadata.contains("family:")) {
            String family = extractFamilyFromMetadata(metadata);
            invalidateTokenFamily(family);
            
            // 보안 알림 (실제로는 이메일/SMS 발송)
            log.error("🚨🚨🚨 TOKEN THEFT DETECTED! Family {} has been invalidated", family);
        }
    }
    
    /**
     * Token Family 전체 무효화
     */
    public void invalidateTokenFamily(String tokenFamily) {
        String familyKey = TOKEN_FAMILY_PREFIX + tokenFamily;
        Set<String> familyTokens = stringRedisTemplate.opsForSet().members(familyKey);
        
        if (familyTokens != null) {
            for (String token : familyTokens) {
                blacklistRefreshToken(token, "FAMILY_INVALIDATED");
                deleteRefreshToken(token);
            }
        }
        
        stringRedisTemplate.delete(familyKey);
        log.warn("Token family {} completely invalidated - {} tokens revoked", 
                tokenFamily, familyTokens != null ? familyTokens.size() : 0);
    }
    
    /**
     * 사용자의 모든 토큰 무효화 (로그아웃)
     */
    public void revokeAllUserTokens(Long memberId) {
        String userKey = USER_TOKENS_PREFIX + memberId;
        Set<String> userTokens = stringRedisTemplate.opsForSet().members(userKey);
        
        if (userTokens != null) {
            for (String token : userTokens) {
                blacklistRefreshToken(token, "USER_LOGOUT");
                deleteRefreshToken(token);
            }
        }
        
        stringRedisTemplate.delete(userKey);
        log.info("All tokens revoked for user: {} ({} tokens)", 
                memberId, userTokens != null ? userTokens.size() : 0);
    }
    
    /**
     * Refresh Token 검증
     */
    public RefreshToken validateRefreshToken(String token) {
        String tokenKey = REFRESH_TOKEN_PREFIX + token;
        RefreshToken refreshToken = (RefreshToken) redisTemplate.opsForValue().get(tokenKey);
        
        if (refreshToken == null) {
            throw new TokenRefreshException("Refresh token not found");
        }
        
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            deleteRefreshToken(token);
            throw new TokenRefreshException("Refresh token has expired");
        }
        
        return refreshToken;
    }
    
    /**
     * JWT Claims에서 남은 TTL 계산
     */
    public long calculateRemainingTTL(Claims claims) {
        Date expiration = claims.getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis);
    }
    
    /**
     * Refresh Token 삭제
     */
    private void deleteRefreshToken(String token) {
        String tokenKey = REFRESH_TOKEN_PREFIX + token;
        redisTemplate.delete(tokenKey);
    }
    
    /**
     * Token 메타데이터 저장 (추적용)
     */
    private void saveTokenMetadata(String token, Long memberId, String family, String action) {
        String metaKey = TOKEN_METADATA_PREFIX + token.substring(0, 8);
        String metadata = String.format("user:%d,family:%s,action:%s,time:%s", 
                memberId, family, action, LocalDateTime.now());
        
        stringRedisTemplate.opsForValue().set(
            metaKey, 
            metadata, 
            refreshTokenExpirationDays + 30,  // 30일 더 보관
            TimeUnit.DAYS
        );
    }
    
    /**
     * 메타데이터에서 Family 추출
     */
    private String extractFamilyFromMetadata(String metadata) {
        String[] parts = metadata.split(",");
        for (String part : parts) {
            if (part.startsWith("family:")) {
                return part.substring(7);
            }
        }
        return null;
    }
    
    /**
     * Redis 연결 상태 확인
     */
    public boolean isRedisAvailable() {
        try {
            stringRedisTemplate.opsForValue().set("health:check", "ok", 1, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Redis connection failed", e);
            return false;
        }
    }
    
    /**
     * Redis 통계 조회 (모니터링용)
     */
    public Map<String, Object> getRedisStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 전체 키 개수 (운영환경에서는 주의)
            Set<String> allKeys = stringRedisTemplate.keys("*");
            stats.put("totalKeys", allKeys != null ? allKeys.size() : 0);
            
            // 각 타입별 개수
            Set<String> refreshTokens = stringRedisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
            Set<String> blacklistedAccess = stringRedisTemplate.keys(ACCESS_BLACKLIST_PREFIX + "*");
            Set<String> blacklistedRefresh = stringRedisTemplate.keys(REFRESH_BLACKLIST_PREFIX + "*");
            
            stats.put("activeRefreshTokens", refreshTokens != null ? refreshTokens.size() : 0);
            stats.put("blacklistedAccessTokens", blacklistedAccess != null ? blacklistedAccess.size() : 0);
            stats.put("blacklistedRefreshTokens", blacklistedRefresh != null ? blacklistedRefresh.size() : 0);
            
        } catch (Exception e) {
            log.error("Failed to get Redis stats", e);
        }
        
        return stats;
    }
}