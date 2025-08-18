package org.moa.global.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.security.dto.RefreshTokenDto;
import org.moa.global.security.dto.TokenInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 생성 및 검증 서비스
 * RTR (Refresh Token Rotation) 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expire-minutes:15}")
    private long accessTokenExpireMinutes;

    @Value("${jwt.refresh-token.expire-days:7}")
    private long refreshTokenExpireDays;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpireMinutes * 60 * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString()) // JTI for blacklist
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성 with Family ID
     */
    public String createRefreshToken(Long memberId, String familyId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpireDays * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("familyId", familyId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString()) // JTI for blacklist
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 새로운 Token Pair 생성 (로그인 시)
     */
    public TokenInfo createTokenPair(Long memberId, HttpServletRequest request) {
        String familyId = UUID.randomUUID().toString();
        String accessToken = createAccessToken(memberId);
        String refreshToken = createRefreshToken(memberId, familyId);

        // Redis에 Refresh Token 정보 저장
        saveRefreshTokenToRedis(memberId, refreshToken, familyId, request);

        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .familyId(familyId)
                .accessTokenExpiresIn(accessTokenExpireMinutes * 60 * 1000)
                .refreshTokenExpiresIn(refreshTokenExpireDays * 24 * 60 * 60 * 1000)
                .build();
    }

    /**
     * Redis에 Refresh Token 저장
     */
    private void saveRefreshTokenToRedis(Long memberId, String token, String familyId, 
                                         HttpServletRequest request) {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("familyId", familyId);
        tokenData.put("issuedAt", System.currentTimeMillis());
        tokenData.put("expiresAt", System.currentTimeMillis() + refreshTokenExpireDays * 24 * 60 * 60 * 1000);
        
        if (request != null) {
            tokenData.put("userAgent", request.getHeader("User-Agent"));
            tokenData.put("ipAddress", getClientIp(request));
        }

        String redisKey = "refresh_token:" + memberId;
        redisTemplate.opsForHash().putAll(redisKey, tokenData);
        redisTemplate.expire(redisKey, refreshTokenExpireDays, TimeUnit.DAYS);

        // Family ID 저장
        redisTemplate.opsForValue().set("family:" + familyId, String.valueOf(memberId));
        redisTemplate.expire("family:" + familyId, refreshTokenExpireDays, TimeUnit.DAYS);
    }

    /**
     * Token 검증 및 Claims 반환
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Token에서 Member ID 추출
     */
    public Long getMemberId(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Token 파싱 (검증 없이)
     */
    public Claims parseToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            
            String payload = new String(decoder.decode(chunks[1]));
            
            // 간단한 파싱을 위해 Claims 객체 대신 맵 사용
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token parsing failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Blacklist에 토큰 추가
     */
    public void addToBlacklist(String token, String type) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return;
            
            String jti = claims.getId();
            if (jti == null) return;
            
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            
            if (ttl > 0) {
                String blacklistKey = "blacklist:" + type + ":" + jti;
                redisTemplate.opsForValue().set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", e.getMessage());
        }
    }

    /**
     * Blacklist 체크
     */
    public boolean isBlacklisted(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return false;
            
            String jti = claims.getId();
            if (jti == null) return false;
            
            String blacklistKey = "blacklist:access:" + jti;
            return redisTemplate.hasKey(blacklistKey);
        } catch (Exception e) {
            log.error("Failed to check blacklist: {}", e.getMessage());
            return false;
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
}
