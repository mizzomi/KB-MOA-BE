package org.moa.global.security.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.domain.RefreshToken;
import org.moa.global.security.dto.TokenRefreshResponseDto;
import org.moa.global.security.exception.TokenRefreshException;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
	
	private final RedisTokenService redisTokenService;
	private final JwtProcessor jwtProcessor;
	
	@Value("${jwt.refresh.cookie.name:refreshToken}")
	private String refreshTokenCookieName;
	
	@Value("${jwt.refresh.cookie.domain:localhost}")
	private String cookieDomain;
	
	@Value("${jwt.refresh.cookie.path:/}")
	private String cookiePath;
	
	@Value("${jwt.refresh.expiration:7}")
	private int refreshTokenExpirationDays;
	
	@Value("${jwt.access.expiration:15}")
	private int accessTokenExpirationMinutes;
	
	@Value("${cookie.secure:false}")
	private boolean cookieSecure;
	
	/**
	 * Access Token 갱신
	 * Cookie에서 Refresh Token을 읽어서 새로운 Access Token 발급
	 */
	@PostMapping("/public/auth/refresh")
	public ResponseEntity<ApiResponse<TokenRefreshResponseDto>> refreshToken(
			HttpServletRequest request, 
			HttpServletResponse response) {
		
		// Cookie에서 Refresh Token 추출
		String refreshTokenValue = extractRefreshTokenFromCookie(request);
		
		if (refreshTokenValue == null) {
			log.warn("Refresh Token이 Cookie에 없음");
			clearRefreshTokenCookie(response);
			throw new TokenRefreshException("Refresh token not found in cookie");
		}
		
		// Refresh Token 검증 (validateRefreshToken에서 예외 발생 시 GlobalExceptionHandler가 처리)
		RefreshToken validToken = redisTokenService.validateRefreshToken(refreshTokenValue);
		
		// Refresh Token Rotation - 새로운 Refresh Token 발급
		RefreshToken newRefreshToken = redisTokenService.rotateRefreshToken(refreshTokenValue);
		
		// 새로운 Access Token 발급
		String newAccessToken = jwtProcessor.generateAccessToken(validToken.getMemberId());
		
		// 새로운 Refresh Token을 Cookie에 설정
		Cookie refreshTokenCookie = createRefreshTokenCookie(newRefreshToken.getToken());
		response.addCookie(refreshTokenCookie);
		
		// Response Body에는 Access Token만 전송
		TokenRefreshResponseDto responseDto = TokenRefreshResponseDto.builder()
				.token(newAccessToken)  // Access Token만 반환
				.expiresIn((long) accessTokenExpirationMinutes * 60)
				.build();
		
		log.info("Token refresh 성공 - memberId: {}", validToken.getMemberId());
		
		return ResponseEntity.ok(ApiResponse.of(responseDto, "Token refreshed successfully"));
	}
	
	/**
	 * 로그아웃
	 * Refresh Token 무효화 및 Cookie 삭제
	 */
	@PostMapping("/auth/logout")
	public ResponseEntity<ApiResponse<String>> logout(
			@AuthenticationPrincipal UserDetails userDetails,
			HttpServletRequest request, 
			HttpServletResponse response) {
		
		// Cookie에서 Refresh Token 추출
		String refreshTokenValue = extractRefreshTokenFromCookie(request);
		
		if (refreshTokenValue != null) {
			// Access Token 블랙리스트 추가 (강제 로그아웃)
			String accessToken = request.getHeader("Authorization");
			if (accessToken != null && accessToken.startsWith("Bearer ")) {
				String token = accessToken.substring(7);
				try {
					// 남은 TTL 계산
					Claims claims = jwtProcessor.getClaims(token);
					long remainingTTL = redisTokenService.calculateRemainingTTL(claims);
					redisTokenService.blacklistAccessToken(token, remainingTTL, "USER_LOGOUT");
				} catch (Exception e) {
					log.debug("Failed to blacklist access token", e);
					// 블랙리스트 실패해도 로그아웃은 계속 진행
				}
			}
			
			// Refresh Token 무효화
			// CustomUser에서 memberId 가져오기
			CustomUser customUser = (CustomUser) userDetails;
			Long memberId = customUser.getMember().getMemberId();
			redisTokenService.revokeAllUserTokens(memberId);
		}
		
		// Cookie 삭제
		clearRefreshTokenCookie(response);
		
		log.info("로그아웃 성공 - user: {}", userDetails.getUsername());
		
		return ResponseEntity.ok(ApiResponse.of("Logout successful"));
	}
	
	/**
	 * 현재 사용자 정보 조회
	 */
	@GetMapping("/auth/me")
	public ResponseEntity<ApiResponse<UserDetails>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
		// SecurityContext에서 현재 사용자 정보 반환
		return ResponseEntity.ok(ApiResponse.of(userDetails));
	}
	
	/**
	 * Cookie에서 Refresh Token 추출
	 */
	private String extractRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (refreshTokenCookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
	
	/**
	 * Refresh Token Cookie 생성
	 */
	private Cookie createRefreshTokenCookie(String refreshToken) {
		Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(cookieSecure);  // 개발환경: false, 운영환경: true
		cookie.setPath(cookiePath);
		cookie.setDomain(cookieDomain);
		cookie.setMaxAge(refreshTokenExpirationDays * 24 * 60 * 60);
		return cookie;
	}
	
	/**
	 * Refresh Token Cookie 삭제
	 */
	private void clearRefreshTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie(refreshTokenCookieName, null);
		cookie.setHttpOnly(true);
		cookie.setSecure(cookieSecure);  // 개발환경: false, 운영환경: true
		cookie.setPath(cookiePath);
		cookie.setDomain(cookieDomain);
		cookie.setMaxAge(0);  // 즉시 만료
		response.addCookie(cookie);
	}
}
