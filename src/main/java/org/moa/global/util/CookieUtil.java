package org.moa.global.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 쿠키 관리 유틸리티 클래스
 * 로컬과 프로덕션 환경에 따라 다른 쿠키 설정 적용
 */
@Component
public class CookieUtil {
    
    @Value("${server.env:local}")
    private String serverEnv;
    
    @Value("${server.use.secure.cookie:false}")
    private boolean useSecureCookie;
    
    /**
     * JWT 토큰을 쿠키로 생성
     * 환경에 따라 Secure, SameSite 속성 자동 설정
     */
    public void createJwtCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie cookie = new Cookie("JWT_TOKEN", token);
        cookie.setHttpOnly(true);  // XSS 방지 (항상 적용)
        cookie.setPath("/");       // 전체 경로에서 사용
        cookie.setMaxAge(maxAge);  // 유효 시간
        
        // 프로덕션 환경에서만 Secure 적용
        if ("production".equals(serverEnv) || useSecureCookie) {
            cookie.setSecure(true);  // HTTPS에서만 전송
        }
        
        // Cookie 객체는 SameSite를 직접 지원하지 않으므로
        // ResponseHeader를 통해 설정
        response.addCookie(cookie);
        
        // SameSite 속성 추가 (Spring 4.x에서는 헤더로 직접 설정)
        String cookieValue = buildCookieHeader(cookie);
        response.addHeader("Set-Cookie", cookieValue);
    }
    
    /**
     * 쿠키 삭제
     */
    public void deleteJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        
        if ("production".equals(serverEnv) || useSecureCookie) {
            cookie.setSecure(true);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Set-Cookie 헤더 문자열 생성
     * SameSite 속성을 포함한 완전한 쿠키 헤더 생성
     */
    private String buildCookieHeader(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
        
        if (cookie.getMaxAge() > 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        
        sb.append("; Path=").append(cookie.getPath());
        sb.append("; HttpOnly");
        
        // 환경별 설정
        if ("production".equals(serverEnv) || useSecureCookie) {
            sb.append("; Secure");
            sb.append("; SameSite=None");  // Cross-Origin 허용 (HTTPS 필수)
        } else {
            sb.append("; SameSite=Lax");   // 로컬 개발 환경
        }
        
        return sb.toString();
    }
    
    /**
     * 리프레시 토큰용 쿠키 (더 긴 유효기간)
     */
    public void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        createJwtCookie(response, refreshToken, 7 * 24 * 60 * 60); // 7일
    }
    
    /**
     * 액세스 토큰용 쿠키 (짧은 유효기간)
     */
    public void createAccessTokenCookie(HttpServletResponse response, String accessToken) {
        createJwtCookie(response, accessToken, 60 * 60); // 1시간
    }
}
