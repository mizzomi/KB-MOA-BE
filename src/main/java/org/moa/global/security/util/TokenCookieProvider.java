package org.moa.global.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Token Cookie 관리 Provider
 * 환경별(로컬/배포) Cookie 설정 관리
 */
@Slf4j
@Component
public class TokenCookieProvider {

    @Value("${app.environment:local}")
    private String environment;

    @Value("${app.cookie.domain:.moa.com}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    /**
     * Refresh Token을 Cookie로 설정
     * 환경에 따라 HTTP/HTTPS 설정 자동 적용
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if ("production".equals(environment) || "docker".equals(environment)) {
            // 배포 환경 - HTTPS only
            setSecureCookie(response, refreshToken);
        } else {
            // 로컬 환경 - HTTP 허용
            setLocalCookie(response, refreshToken);
        }
    }

    /**
     * 배포 환경용 Secure Cookie 설정
     */
    private void setSecureCookie(HttpServletResponse response, String refreshToken) {
        // Spring Legacy에서는 SameSite 속성을 직접 헤더로 설정
        String cookieValue = String.format(
            "%s=%s; Path=/; Domain=%s; Max-Age=%d; HttpOnly; Secure; SameSite=Strict",
            REFRESH_TOKEN_COOKIE_NAME,
            refreshToken,
            cookieDomain,
            REFRESH_TOKEN_MAX_AGE
        );
        
        response.addHeader("Set-Cookie", cookieValue);
        log.debug("Set secure refresh token cookie for production environment");
    }

    /**
     * 로컬 환경용 Cookie 설정
     */
    private void setLocalCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);  // JavaScript 접근 차단
        cookie.setSecure(false);   // HTTP 허용
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        
        response.addCookie(cookie);
        log.debug("Set local refresh token cookie for development environment");
    }

    /**
     * Refresh Token Cookie 삭제
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        if ("production".equals(environment) || "docker".equals(environment)) {
            // 배포 환경
            String cookieValue = String.format(
                "%s=; Path=/; Domain=%s; Max-Age=0; HttpOnly; Secure; SameSite=Strict",
                REFRESH_TOKEN_COOKIE_NAME,
                cookieDomain
            );
            response.addHeader("Set-Cookie", cookieValue);
        } else {
            // 로컬 환경
            Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
        
        log.debug("Cleared refresh token cookie");
    }

    /**
     * Cookie에서 Refresh Token 추출
     */
    public String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        
        return null;
    }
}
