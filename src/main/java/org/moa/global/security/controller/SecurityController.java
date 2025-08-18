package org.moa.global.security.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.dto.TokenInfo;
import org.moa.global.security.service.JwtTokenService;
import org.moa.global.security.service.TokenRotationService;
import org.moa.global.security.util.TokenCookieProvider;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SecurityController {

	private final TokenRotationService tokenRotationService;
	private final TokenCookieProvider tokenCookieProvider;
	private final JwtTokenService jwtTokenService;

	// ========== 기존 엔드포인트 ==========
	
	@GetMapping("/public/all") // 모두 접근 가능
	public ResponseEntity<String> doAll() {
		log.info("do all can access everybody");
		LocalDateTime now = LocalDateTime.now();
		return ResponseEntity.ok("all can access everybody, now is " + now);
	}

	@GetMapping("/member")
	public ResponseEntity<String> doMember(Authentication authentication) {
		UserDetails userDetails = (UserDetails)authentication.getPrincipal();
		log.info("username = " + userDetails.getUsername());
		return ResponseEntity.ok(userDetails.getUsername());
	}

	@GetMapping("/admin")
	public ResponseEntity<Member> doAdmin(@AuthenticationPrincipal CustomUser customUser) {
		Member member = customUser.getMember();
		log.info("username = " + member);
		return ResponseEntity.ok(member);
	}

	@GetMapping("/member/findById")
	public ResponseEntity<ApiResponse<Member>> findMember(Authentication authentication) {
		CustomUser customuser = (CustomUser)authentication.getPrincipal();
		Member member = customuser.getMember();

		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(member));
	}

	// ========== RTR 관련 엔드포인트 ==========

	/**
	 * Token Refresh Endpoint - 모든 인증된 사용자 접근 가능
	 * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급
	 */
	@PostMapping("/public/auth/refresh")
	public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
			@CookieValue(value = "refreshToken", required = false) String refreshToken,
			HttpServletRequest request,
			HttpServletResponse response) {

		try {
			// 1. Refresh Token 검증
			if (refreshToken == null || refreshToken.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ApiResponse.error(StatusCode.UNAUTHORIZED));
			}

			// 2. Token Rotation 수행
			TokenInfo newTokens = tokenRotationService.rotateToken(refreshToken, request);

			// 3. 새 Refresh Token을 Cookie로 설정
			tokenCookieProvider.setRefreshTokenCookie(response, newTokens.getRefreshToken());

			// 4. Access Token은 Response Body로 반환 (ApiResponse 형식)
			Map<String, Object> data = new HashMap<>();
			data.put("accessToken", newTokens.getAccessToken());
			data.put("tokenType", newTokens.getTokenType());
			data.put("expiresIn", newTokens.getAccessTokenExpiresIn());

			log.info("Token refreshed successfully");
			return ResponseEntity.ok(ApiResponse.of(data, "토큰이 갱신되었습니다."));

		} catch (SecurityException e) {
			log.error("Token refresh failed: {}", e.getMessage());
			
			// 보안 예외 발생 시 Cookie 삭제
			tokenCookieProvider.clearRefreshTokenCookie(response);
			
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ApiResponse.error(StatusCode.UNAUTHORIZED));
		} catch (Exception e) {
			log.error("Unexpected error during token refresh: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.error(StatusCode.INTERNAL_ERROR));
		}
	}

	/**
	 * Logout Endpoint - 모든 인증된 사용자 접근 가능
	 * Access Token과 Refresh Token 모두 무효화
	 */
	@PostMapping("/member/auth/logout")
	public ResponseEntity<ApiResponse<String>> logout(
			@RequestHeader(value = "Authorization", required = false) String bearerToken,
			@CookieValue(value = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response) {

		try {
			// 1. 현재 인증 정보 가져오기
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			Long memberId = null;
			
			if (authentication != null && authentication.isAuthenticated()) {
				try {
					// CustomUser에서 memberId 추출
					if (authentication.getPrincipal() instanceof CustomUser) {
						CustomUser customUser = (CustomUser) authentication.getPrincipal();
						memberId = customUser.getMember().getMemberId();
					} else {
						memberId = Long.parseLong(authentication.getName());
					}
				} catch (Exception e) {
					log.warn("Failed to parse member ID from authentication: {}", e.getMessage());
				}
			}

			// 2. Access Token 추출
			String accessToken = null;
			if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
				accessToken = bearerToken.substring(7);
			}

			// 3. 로그아웃 처리
			if (memberId != null) {
				tokenRotationService.logout(memberId, accessToken, refreshToken);
			} else if (accessToken != null) {
				// Member ID를 못 가져온 경우 토큰에서 추출 시도
				try {
					memberId = jwtTokenService.getMemberId(accessToken);
					tokenRotationService.logout(memberId, accessToken, refreshToken);
				} catch (Exception e) {
					log.warn("Failed to extract member ID from token: {}", e.getMessage());
					// 최소한 블랙리스트에는 추가
					jwtTokenService.addToBlacklist(accessToken, "access");
					if (refreshToken != null) {
						jwtTokenService.addToBlacklist(refreshToken, "refresh");
					}
				}
			}

			// 4. Cookie 삭제
			tokenCookieProvider.clearRefreshTokenCookie(response);

			// 5. Security Context 클리어
			SecurityContextHolder.clearContext();

			log.info("User logged out successfully");
			return ResponseEntity.ok(ApiResponse.of("로그아웃되었습니다."));

		} catch (Exception e) {
			log.error("Logout failed: {}", e.getMessage());
			// 에러가 발생해도 Cookie는 삭제
			tokenCookieProvider.clearRefreshTokenCookie(response);
			return ResponseEntity.ok(ApiResponse.of("로그아웃되었습니다."));
		}
	}

	/**
	 * Token 유효성 검증 Endpoint - Public 접근 가능
	 * 현재 Access Token이 유효한지 확인
	 */
	@GetMapping("/public/auth/validate")
	public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
			@RequestHeader(value = "Authorization", required = false) String bearerToken) {

		Map<String, Object> result = new HashMap<>();
		
		try {
			if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
				result.put("valid", false);
				result.put("reason", "Token not provided");
				return ResponseEntity.ok(ApiResponse.of(result));
			}

			String accessToken = bearerToken.substring(7);
			
			// 블랙리스트 체크
			if (jwtTokenService.isBlacklisted(accessToken)) {
				result.put("valid", false);
				result.put("reason", "Token is blacklisted");
				return ResponseEntity.ok(ApiResponse.of(result));
			}

			// 토큰 검증
			jwtTokenService.validateToken(accessToken);
			result.put("valid", true);
			
			return ResponseEntity.ok(ApiResponse.of(result));

		} catch (Exception e) {
			result.put("valid", false);
			result.put("reason", e.getMessage());
			return ResponseEntity.ok(ApiResponse.of(result));
		}
	}

	/**
	 * 모든 토큰 무효화 Endpoint - 인증된 사용자만 접근 가능
	 * 사용자 요청 시 해당 사용자의 모든 토큰 무효화
	 */
	@PostMapping("/member/auth/invalidate-all")
	public ResponseEntity<ApiResponse<String>> invalidateAllTokens(
			HttpServletResponse response) {

		try {
			// 현재 인증된 사용자 정보 가져오기
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			
			if (authentication == null || !authentication.isAuthenticated()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ApiResponse.error(StatusCode.UNAUTHORIZED));
			}

			Long memberId = null;
			if (authentication.getPrincipal() instanceof CustomUser) {
				CustomUser customUser = (CustomUser) authentication.getPrincipal();
				memberId = customUser.getMember().getMemberId();
			} else {
				memberId = Long.parseLong(authentication.getName());
			}
			
			// 모든 토큰 무효화
			tokenRotationService.invalidateAllTokens(memberId);
			
			// Cookie 삭제
			tokenCookieProvider.clearRefreshTokenCookie(response);
			
			// Security Context 클리어
			SecurityContextHolder.clearContext();

			log.info("All tokens invalidated for member: {}", memberId);
			return ResponseEntity.ok(ApiResponse.of("모든 토큰이 무효화되었습니다."));

		} catch (Exception e) {
			log.error("Failed to invalidate all tokens: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.error(StatusCode.INTERNAL_ERROR));
		}
	}

	/**
	 * Owner 권한 테스트 엔드포인트
	 */
	@GetMapping("/owner/test")
	public ResponseEntity<ApiResponse<String>> ownerTest(@AuthenticationPrincipal CustomUser customUser) {
		Member member = customUser.getMember();
		log.info("Owner access: {}", member.getEmail());
		return ResponseEntity.ok(ApiResponse.of("Owner 권한 테스트 성공"));
	}

	/**
	 * Admin 권한 테스트 엔드포인트
	 */
	@GetMapping("/admin/test")
	public ResponseEntity<ApiResponse<String>> adminTest(@AuthenticationPrincipal CustomUser customUser) {
		Member member = customUser.getMember();
		log.info("Admin access: {}", member.getEmail());
		return ResponseEntity.ok(ApiResponse.of("Admin 권한 테스트 성공"));
	}
}
