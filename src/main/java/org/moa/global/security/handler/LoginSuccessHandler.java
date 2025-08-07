package org.moa.global.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.domain.RefreshToken;
import org.moa.global.security.dto.AuthResultDto;
import org.moa.global.security.dto.UserInfoDto;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JsonResponse;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtProcessor jwtProcessor;
	private final RedisTokenService redisTokenService;  // Redis 서비스로 변경
	
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

	private AuthResultDto makeAuthResult(CustomUser user) {
		Long memberId = user.getMember().getMemberId();
		
		// Access Token 생성
		String accessToken = jwtProcessor.generateAccessToken(memberId);
		log.info("Access Token 생성 완료 - memberId: {}", memberId);
		
		// Refresh Token 생성 (Redis에 저장)
		try {
			RefreshToken refreshToken = redisTokenService.createRefreshToken(memberId);
			log.info("Refresh Token 생성 완료 - token: {}..., family: {}...", 
					refreshToken.getToken().substring(0, 8), 
					refreshToken.getTokenFamily().substring(0, 8));
			
			// Access Token 만료 시간 (초 단위)
			long expiresIn = accessTokenExpirationMinutes * 60L;
			
			return AuthResultDto.builder()
					.accessToken(accessToken)
					.refreshToken(refreshToken.getToken())
					.user(UserInfoDto.of(user.getMember()))
					.expiresIn(expiresIn)
					.build();
		} catch (Exception e) {
			log.error("Refresh Token 생성 실패", e);
			throw new RuntimeException("Failed to create refresh token", e);
		}
	}
	
	private Cookie createRefreshTokenCookie(String refreshToken) {
		Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
		
		// Cookie 설정
		cookie.setHttpOnly(true);  // XSS 공격 방지
		cookie.setSecure(cookieSecure);  // HTTPS에서만 전송 (개발환경에서는 false)
		cookie.setPath(cookiePath);
		cookie.setDomain(cookieDomain);
		cookie.setMaxAge(refreshTokenExpirationDays * 24 * 60 * 60);  // 7일
		
		// SameSite 설정 (Spring Boot 2.6+ 에서 지원)
		// cookie.setSameSite("Strict");  // CSRF 공격 방지
		
		return cookie;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		
		// 인증 결과 Principal
		CustomUser user = (CustomUser) authentication.getPrincipal();
		
		// 클라이언트 정보 추출
		String ipAddress = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		
		// 인증 토큰 생성
		AuthResultDto result = makeAuthResult(user);
		
		// Refresh Token을 HttpOnly Cookie로 설정
		Cookie refreshTokenCookie = createRefreshTokenCookie(result.getRefreshToken());
		response.addCookie(refreshTokenCookie);
		
		// Response Body에는 Access Token과 사용자 정보만 전송
		AuthResultDto responseBody = AuthResultDto.builder()
				.accessToken(result.getAccessToken())
				.user(result.getUser())
				.expiresIn(result.getExpiresIn())
				.build();
		
		// 로그인 성공 로그
		log.info("로그인 성공 - memberId: {}, email: {}, IP: {}", 
				user.getMember().getMemberId(), user.getMember().getEmail(), ipAddress);
		
		// JSON 응답
		JsonResponse.send(response, responseBody);
	}
	
	/**
	 * 클라이언트 IP 추출
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
				return ip.split(",")[0].trim();
			}
		}
		
		return request.getRemoteAddr();
	}
}
