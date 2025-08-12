package org.moa.global.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.dto.TokenDto;
import org.moa.global.security.service.AuthService;
import org.moa.global.type.StatusCode;
import org.moa.global.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "https://kb-moa-fe.vercel.app"}, 
             allowCredentials = "true")
public class AuthController {
    
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    
    /**
     * Access Token 갱신
     * - Refresh Token은 HttpOnly 쿠키에서만 읽음 (보안)
     * - 새로운 Access Token은 응답 본문으로만 전달
     * - 새로운 Refresh Token은 HttpOnly 쿠키로 자동 갱신 (RTR)
     */
    @PostMapping("/public/auth/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            // Refresh Token은 HttpOnly 쿠키에서만 추출 (보안)
            String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
            
            if (refreshToken == null) {
                log.warn("Refresh Token 쿠키가 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(StatusCode.UNAUTHORIZED, "Refresh Token이 없습니다. 다시 로그인해주세요."));
            }
            
            // Token 갱신 (RTR 적용 - Family ID를 통한 탈취 감지)
            TokenDto newTokens = authService.refreshTokenWithRotation(refreshToken);
            
            // 새로운 Refresh Token을 HttpOnly 쿠키에 저장 (RTR)
            cookieUtil.createRefreshTokenCookie(response, newTokens.getRefreshToken());
            
            // Access Token은 응답 본문으로만 전달
            TokenDto responseDto = TokenDto.builder()
                .accessToken(newTokens.getAccessToken())
                .refreshToken(null)  // 보안상 응답에서 제외
                .accessTokenExpiresIn(newTokens.getAccessTokenExpiresIn())
                .refreshTokenExpiresIn(newTokens.getRefreshTokenExpiresIn())
                .tokenType("Bearer")
                .build();
            
            log.info("토큰 갱신 성공 (RTR 적용) - AT: Response Body, RT: HttpOnly Cookie");
            
            return ResponseEntity.ok(ApiResponse.of(responseDto, "토큰이 갱신되었습니다."));
            
        } catch (SecurityException e) {
            log.error("보안 위협 감지 (Token Family 불일치): {}", e.getMessage());
            
            // 보안 위협 감지 시 모든 쿠키 삭제
            cookieUtil.deleteAllAuthCookies(response);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(StatusCode.UNAUTHORIZED, "보안 위협이 감지되었습니다. 다시 로그인해주세요."));
            
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(StatusCode.UNAUTHORIZED, "토큰 갱신에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 로그아웃
     * Authorization 헤더의 Access Token을 블랙리스트에 추가
     * Redis에서 Refresh Token 삭제
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            // Authorization 헤더에서 Access Token 추출
            String accessToken = extractAccessTokenFromHeader(request);
            
            // 레거시 지원: 헤더에 없으면 쿠키 확인
            if (accessToken == null) {
                accessToken = cookieUtil.getCookieValue(request, "JWT_TOKEN");
            }
            
            if (accessToken != null) {
                // 로그아웃 처리
                // 1. Access Token을 블랙리스트에 추가
                // 2. Redis에서 Refresh Token 삭제
                authService.logout(accessToken);
                log.info("로그아웃 처리 완료 - 블랙리스트 추가 및 RT 삭제");
            }
            
            // Refresh Token 쿠키 삭제
            cookieUtil.deleteAllAuthCookies(response);
            
            // SecurityContext 초기화
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(ApiResponse.<Void>of(null, "로그아웃되었습니다."));
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류: {}", e.getMessage());
            // 오류가 발생해도 쿠키는 삭제
            cookieUtil.deleteAllAuthCookies(response);
            return ResponseEntity.ok(ApiResponse.<Void>of(null, "로그아웃되었습니다."));
        }
    }
    
    /**
     * 토큰 유효성 검증
     * Authorization 헤더의 Access Token을 검증
     */
    @GetMapping("/auth/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String accessToken = extractAccessTokenFromHeader(request);
            
            if (accessToken == null) {
                return ResponseEntity.ok(ApiResponse.of(false, "토큰이 없습니다."));
            }
            
            boolean isValid = authService.validateAccessToken(accessToken);
            
            return ResponseEntity.ok(ApiResponse.of(isValid, 
                isValid ? "유효한 토큰입니다." : "유효하지 않은 토큰입니다."));
            
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.of(false, "토큰 검증 실패"));
        }
    }
    
    /**
     * Authorization 헤더에서 Access Token 추출
     */
    private String extractAccessTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
