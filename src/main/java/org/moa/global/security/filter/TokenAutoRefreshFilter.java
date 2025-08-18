package org.moa.global.security.filter;

import java.io.IOException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.dto.TokenInfo;
import org.moa.global.security.service.JwtTokenService;
import org.moa.global.security.service.TokenRotationService;
import org.moa.global.security.util.TokenCookieProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Access Token 자동 갱신 필터
 * Access Token이 만료되었지만 Refresh Token이 유효한 경우 자동으로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAutoRefreshFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000; // 5분
    
    private final JwtTokenService jwtTokenService;
    private final TokenRotationService tokenRotationService;
    private final TokenCookieProvider tokenCookieProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        // OPTIONS 요청이나 public 경로는 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || 
            isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        // Access Token이 없으면 통과
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = bearerToken.substring(BEARER_PREFIX.length());
        
        try {
            // Access Token 파싱 (검증은 하지 않음)
            Claims claims = jwtTokenService.parseToken(accessToken);
            
            if (claims != null) {
                Date expiration = claims.getExpiration();
                long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();
                
                // 토큰이 만료되었거나 곧 만료될 예정인 경우
                if (timeUntilExpiry <= TOKEN_REFRESH_THRESHOLD) {
                    log.debug("Access token expired or expiring soon, attempting auto-refresh");
                    
                    // Refresh Token 가져오기
                    String refreshToken = getRefreshTokenFromCookie(request);
                    
                    if (refreshToken != null) {
                        try {
                            // 토큰 갱신
                            TokenInfo newTokens = tokenRotationService.rotateToken(refreshToken, request);
                            
                            // 새 Refresh Token을 Cookie로 설정
                            tokenCookieProvider.setRefreshTokenCookie(response, newTokens.getRefreshToken());
                            
                            // 새 Access Token을 Response Header에 추가
                            response.setHeader("X-New-Access-Token", newTokens.getAccessToken());
                            response.setHeader("X-Token-Refreshed", "true");
                            
                            // Request를 Wrapper로 감싸서 새 토큰으로 교체
                            HttpServletRequest wrappedRequest = new TokenReplacedRequestWrapper(
                                request, BEARER_PREFIX + newTokens.getAccessToken()
                            );
                            
                            log.info("Token auto-refreshed successfully");
                            filterChain.doFilter(wrappedRequest, response);
                            return;
                            
                        } catch (Exception e) {
                            log.warn("Auto-refresh failed: {}", e.getMessage());
                            // 갱신 실패해도 기존 요청은 계속 처리
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Token parsing failed, continuing with original request: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Cookie에서 Refresh Token 추출
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Public 경로 확인
     */
    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/api/public/") ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.equals("/") ||
               requestURI.startsWith("/favicon");
    }
    
    /**
     * Request Wrapper - Authorization 헤더를 새 토큰으로 교체
     */
    private static class TokenReplacedRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
        private final String newToken;
        
        public TokenReplacedRequestWrapper(HttpServletRequest request, String newToken) {
            super(request);
            this.newToken = newToken;
        }
        
        @Override
        public String getHeader(String name) {
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                return newToken;
            }
            return super.getHeader(name);
        }
    }
}
