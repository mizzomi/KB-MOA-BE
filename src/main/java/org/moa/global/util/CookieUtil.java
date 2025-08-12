package org.moa.global.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠키 관리 유틸리티 클래스
 * - Access Token: Response Body로 전달
 * - Refresh Token: HttpOnly 쿠키로 전달
 * - 로컬: HTTP, 배포: HTTPS (Nginx)
 */
@Slf4j
@Component
public class CookieUtil {
    
    @Value("${server.env:local}")
    private String serverEnv;
    
    @Value("${server.use.secure.cookie:false}")
    private boolean useSecureCookie;
    
    // 쿠키 이름 상수
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    
    /**
     * Refresh Token을 HttpOnly 쿠키로 생성 (보안 강화)
     * 프론트엔드에서 접근 불가, 자동으로 서버에 전송
     */
    public void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);  // JavaScript에서 접근 불가 (XSS 방지)
        cookie.setPath("/");       // 전체 경로에서 사용
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        
        // 배포 환경(HTTPS) 설정
        if ("production".equals(serverEnv) || useSecureCookie) {
            cookie.setSecure(true);  // HTTPS에서만 전송
        }
        
        response.addCookie(cookie);
        
        // SameSite 속성 추가 (직접 헤더로 설정)
        String cookieHeader = buildRefreshTokenCookieHeader(cookie);
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("Refresh Token 쿠키 생성 - HttpOnly: true, Secure: {}, 환경: {}", 
            cookie.getSecure(), serverEnv);
    }
    
    /**
     * 쿠키 삭제
     */
    public void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        cookie.setHttpOnly(true);  // Refresh Token은 항상 HttpOnly
        
        if ("production".equals(serverEnv) || useSecureCookie) {
            cookie.setSecure(true);
        }
        
        response.addCookie(cookie);
        log.debug("쿠키 삭제: {}", cookieName);
    }
    
    /**
     * 요청에서 쿠키 값 추출
     */
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Refresh Token용 Set-Cookie 헤더 생성
     */
    private String buildRefreshTokenCookieHeader(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
        
        if (cookie.getMaxAge() > 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        
        sb.append("; Path=").append(cookie.getPath());
        sb.append("; HttpOnly"); // 항상 HttpOnly
        
        // 환경별 설정
        if ("production".equals(serverEnv) || useSecureCookie) {
            // 배포: HTTPS (Nginx가 SSL 처리)
            sb.append("; Secure");
            sb.append("; SameSite=None");  // Cross-Origin 허용 (HTTPS 필수)
        } else {
            // 로컬: HTTP
            sb.append("; SameSite=Lax");   // CSRF 방지하면서 일반 링크는 허용
        }
        
        return sb.toString();
    }
    
    /**
     * Refresh Token 쿠키 삭제
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        deleteCookie(response, REFRESH_TOKEN_COOKIE);
    }
    
    /**
     * 모든 인증 관련 쿠키 삭제
     */
    public void deleteAllAuthCookies(HttpServletResponse response) {
        deleteRefreshTokenCookie(response);
        // 레거시 JWT_TOKEN 쿠키도 삭제 (하위 호환성)
        deleteCookie(response, "JWT_TOKEN");
        log.info("모든 인증 관련 쿠키 삭제 완료");
    }
}
