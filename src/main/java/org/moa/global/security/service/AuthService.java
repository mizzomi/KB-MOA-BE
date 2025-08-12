package org.moa.global.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.security.dto.TokenDto;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtProcessor jwtProcessor;
    private final RedisTokenService redisTokenService;
    
    /**
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급 (RTR)
     * @param refreshToken 현재 Refresh Token
     * @return 새로운 Token 쌍
     */
    @Transactional
    public TokenDto refreshTokenWithRotation(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProcessor.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }
        
        // 2. Token 타입 확인
        String tokenType = jwtProcessor.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }
        
        // 3. Refresh Token에서 정보 추출
        Long memberId = jwtProcessor.getMemberId(refreshToken);
        String familyId = jwtProcessor.getFamilyId(refreshToken);
        
        // 4. Redis에서 Refresh Token 검증 (RTR - 탈취 감지)
        if (!redisTokenService.validateRefreshToken(memberId, refreshToken)) {
            log.error("Refresh Token 탈취 감지 - memberId: {}, familyId: {}", memberId, familyId);
            throw new SecurityException("보안 위협이 감지되었습니다. 다시 로그인해주세요.");
        }
        
        // 5. 새로운 Access Token 생성
        String newAccessToken = jwtProcessor.generateAccessToken(memberId);
        
        // 6. 새로운 Refresh Token 생성 (Rotation)
        String newRefreshToken = jwtProcessor.generateRefreshToken(memberId, familyId);
        
        // 7. Redis에 새로운 Refresh Token 저장 (기존 Family ID 유지)
        redisTokenService.saveRefreshToken(memberId, newRefreshToken, familyId);
        
        log.info("Token Rotation 성공 - memberId: {}, familyId: {}", memberId, familyId);
        
        // 8. 결과 반환
        return TokenDto.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .accessTokenExpiresIn(jwtProcessor.getAccessTokenValidTime())
            .refreshTokenExpiresIn(jwtProcessor.getRefreshTokenValidTime())
            .tokenType("Bearer")
            .build();
    }
    
    /**
     * 로그아웃 처리
     * @param accessToken 현재 Access Token
     */
    @Transactional
    public void logout(String accessToken) {
        try {
            // Access Token에서 memberId 추출
            Long memberId = jwtProcessor.getMemberId(accessToken);
            
            // Access Token을 블랙리스트에 추가
            redisTokenService.addToBlacklist(accessToken, memberId);
            
            // Refresh Token 삭제
            redisTokenService.deleteRefreshToken(memberId);
            
            log.info("로그아웃 처리 완료 - memberId: {}", memberId);
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류: {}", e.getMessage());
            throw new RuntimeException("로그아웃 처리 실패", e);
        }
    }
    
    /**
     * Access Token 유효성 검증
     * @param accessToken 검증할 Access Token
     * @return 유효 여부
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. 블랙리스트 체크
            if (redisTokenService.isBlacklisted(accessToken)) {
                log.warn("블랙리스트에 있는 토큰");
                return false;
            }
            
            // 2. 토큰 유효성 검증
            if (!jwtProcessor.validateToken(accessToken)) {
                return false;
            }
            
            // 3. 토큰 타입 확인
            String tokenType = jwtProcessor.getTokenType(accessToken);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("Access Token이 아님: {}", tokenType);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 보안 위협 감지 시 사용자의 모든 토큰 무효화
     * @param memberId 사용자 ID
     */
    @Transactional
    public void revokeAllTokens(Long memberId) {
        try {
            // Refresh Token 및 Family 삭제
            redisTokenService.deleteRefreshToken(memberId);
            
            log.warn("사용자의 모든 토큰 무효화 - memberId: {}", memberId);
        } catch (Exception e) {
            log.error("토큰 무효화 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 무효화 실패", e);
        }
    }
}
