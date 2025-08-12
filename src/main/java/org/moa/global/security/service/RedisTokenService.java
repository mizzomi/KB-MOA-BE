package org.moa.global.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {
    
    private final RedisTemplate<String, String> stringRedisTemplate;
    
    // Token 유효시간 설정
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 15; // 15분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7; // 7일
    private static final long BLACKLIST_EXPIRE_TIME = 24; // 24시간 (Access Token 최대 유효시간보다 길게)
    
    // Redis Key Prefix
    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";
    private static final String REFRESH_TOKEN_FAMILY_PREFIX = "RTF:";
    
    /**
     * Refresh Token 저장 (Rotation 지원)
     * @param memberId 사용자 ID
     * @param refreshToken 새로운 Refresh Token
     * @param familyId Token Family ID (RTR을 위한 그룹 ID)
     */
    public void saveRefreshToken(Long memberId, String refreshToken, String familyId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        String familyKey = REFRESH_TOKEN_FAMILY_PREFIX + familyId;
        
        // Refresh Token 저장 (value: familyId)
        stringRedisTemplate.opsForValue().set(key, familyId, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.DAYS);
        
        // Token Family에 현재 유효한 Refresh Token 저장
        stringRedisTemplate.opsForValue().set(familyKey, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.DAYS);
        
        log.info("Refresh Token 저장 완료 - memberId: {}, familyId: {}", memberId, familyId);
    }
    
    /**
     * Refresh Token Family ID 조회
     * @param memberId 사용자 ID
     * @return Family ID
     */
    public String getRefreshTokenFamilyId(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return stringRedisTemplate.opsForValue().get(key);
    }
    
    /**
     * Refresh Token Family의 현재 유효한 토큰 조회
     * @param familyId Token Family ID
     * @return 현재 유효한 Refresh Token
     */
    public String getCurrentRefreshToken(String familyId) {
        String familyKey = REFRESH_TOKEN_FAMILY_PREFIX + familyId;
        return stringRedisTemplate.opsForValue().get(familyKey);
    }
    
    /**
     * Refresh Token 검증 (RTR 적용)
     * @param memberId 사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @return 유효 여부
     */
    public boolean validateRefreshToken(Long memberId, String refreshToken) {
        String familyId = getRefreshTokenFamilyId(memberId);
        if (familyId == null) {
            log.warn("Refresh Token Family ID가 존재하지 않음 - memberId: {}", memberId);
            return false;
        }
        
        String currentToken = getCurrentRefreshToken(familyId);
        boolean isValid = refreshToken.equals(currentToken);
        
        if (!isValid) {
            log.warn("유효하지 않은 Refresh Token (탈취 의심) - memberId: {}, familyId: {}", memberId, familyId);
            // 탈취가 의심되면 해당 Family의 모든 토큰 무효화
            invalidateTokenFamily(memberId, familyId);
        }
        
        return isValid;
    }
    
    /**
     * Token Family 무효화 (탈취 감지 시)
     * @param memberId 사용자 ID
     * @param familyId Token Family ID
     */
    public void invalidateTokenFamily(Long memberId, String familyId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        String familyKey = REFRESH_TOKEN_FAMILY_PREFIX + familyId;
        
        stringRedisTemplate.delete(key);
        stringRedisTemplate.delete(familyKey);
        
        log.warn("Token Family 무효화 완료 (보안 위협 감지) - memberId: {}, familyId: {}", memberId, familyId);
    }
    
    /**
     * Refresh Token 삭제 (로그아웃)
     * @param memberId 사용자 ID
     */
    public void deleteRefreshToken(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        String familyId = stringRedisTemplate.opsForValue().get(key);
        
        if (familyId != null) {
            String familyKey = REFRESH_TOKEN_FAMILY_PREFIX + familyId;
            stringRedisTemplate.delete(familyKey);
        }
        
        stringRedisTemplate.delete(key);
        log.info("Refresh Token 삭제 완료 - memberId: {}", memberId);
    }
    
    /**
     * Access Token을 블랙리스트에 추가
     * @param accessToken 블랙리스트에 추가할 Access Token
     * @param memberId 사용자 ID
     */
    public void addToBlacklist(String accessToken, Long memberId) {
        String key = BLACKLIST_PREFIX + accessToken;
        stringRedisTemplate.opsForValue().set(
            key, 
            String.valueOf(memberId), 
            BLACKLIST_EXPIRE_TIME, 
            TimeUnit.HOURS
        );
        log.info("Access Token 블랙리스트 추가 - memberId: {}", memberId);
    }
    
    /**
     * Access Token이 블랙리스트에 있는지 확인
     * @param accessToken 확인할 Access Token
     * @return 블랙리스트 포함 여부
     */
    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return stringRedisTemplate.hasKey(key);
    }
    
    /**
     * 새로운 Token Family ID 생성
     * @return 새로운 Family ID
     */
    public String generateFamilyId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Access Token 만료 시간 (분 단위)
     */
    public long getAccessTokenExpireTime() {
        return ACCESS_TOKEN_EXPIRE_TIME;
    }
    
    /**
     * Refresh Token 만료 시간 (일 단위)
     */
    public long getRefreshTokenExpireTime() {
        return REFRESH_TOKEN_EXPIRE_TIME;
    }
}
