package org.moa.global.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.dto.AuthResultDto;
import org.moa.global.security.dto.TokenInfo;
import org.moa.global.security.dto.UserInfoDto;
import org.moa.global.security.service.JwtTokenService;
import org.moa.global.security.util.JsonResponse;
import org.moa.global.security.util.JwtProcessor;
import org.moa.global.security.util.TokenCookieProvider;
import org.moa.global.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 성공 핸들러
 * RTR (Refresh Token Rotation) 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtProcessor jwtProcessor;
	private final JwtTokenService jwtTokenService;
	private final TokenCookieProvider tokenCookieProvider;
	private final CookieUtil cookieUtil;
	
	@Autowired(required = false)
	private RedisTemplate<String, Object> redisTemplate;

	/**
	 * 기존 방식의 인증 결과 생성 (하위 호환성 유지)
	 */
	private AuthResultDto makeAuthResult(CustomUser user) {
		Long memberId = user.getMember().getMemberId();
		String token = jwtProcessor.generateToken(memberId);
		return new AuthResultDto(token, UserInfoDto.of(user.getMember()));
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		
		log.debug("onAuthenticationSuccess called");
		
		// Response가 이미 커밋되었는지 확인
		if (response.isCommitted()) {
			log.warn("Response already committed at the start of onAuthenticationSuccess");
			return;
		}
		
		// 인증 결과 Principal
		CustomUser user = (CustomUser)authentication.getPrincipal();
		Long memberId = user.getMember().getMemberId();
		
		log.debug("Processing login for memberId: {}", memberId);
		
		// Redis 연결 체크
		boolean useRTR = false;
		try {
			if (redisTemplate != null) {
				// Redis 연결 테스트
				redisTemplate.opsForValue().set("ping", "pong", 1, java.util.concurrent.TimeUnit.SECONDS);
				useRTR = true;
				log.debug("Redis is available, using RTR");
			}
		} catch (Exception e) {
			log.warn("Redis not available, falling back to legacy auth: {}", e.getMessage());
			useRTR = false;
		}
		
		try {
			// 응답 상태 설정
			response.setStatus(HttpServletResponse.SC_OK);
			
			if (useRTR) {
				// RTR 적용 - Token Pair 생성
				TokenInfo tokenInfo = jwtTokenService.createTokenPair(memberId, request);
				
				// Refresh Token을 HttpOnly Cookie로 설정
				tokenCookieProvider.setRefreshTokenCookie(response, tokenInfo.getRefreshToken());
				
				// ApiResponse 형식에 맞게 응답 데이터 구성
				Map<String, Object> responseData = new HashMap<>();
				responseData.put("accessToken", tokenInfo.getAccessToken());
				responseData.put("tokenType", tokenInfo.getTokenType());
				responseData.put("expiresIn", tokenInfo.getAccessTokenExpiresIn());
				responseData.put("userInfo", UserInfoDto.of(user.getMember()));
				
				// ApiResponse 형식으로 JSON 응답
				ApiResponse<Map<String, Object>> apiResponse = ApiResponse.of(responseData, "로그인 성공");
				
				// 응답 전송
				JsonResponse.send(response, apiResponse);
				
				log.info("로그인 성공 (RTR 적용): memberId={}, Refresh Token Cookie 설정 완료", memberId);
				
			} else {
				// Redis가 없거나 RTR을 사용할 수 없는 경우 기존 방식 사용
				sendFallbackResponse(response, user);
			}
		} catch (Exception e) {
			log.error("Error during authentication success handling: {}", e.getMessage(), e);
			
			// Response가 커밋되지 않았고, 에러가 발생한 경우에만 폴백
			if (!response.isCommitted()) {
				try {
					sendFallbackResponse(response, user);
				} catch (Exception fallbackError) {
					log.error("Fallback also failed: {}", fallbackError.getMessage());
					// 최소한의 응답이라도 전송
					if (!response.isCommitted()) {
						response.setStatus(HttpServletResponse.SC_OK);
						response.setContentType("application/json");
						response.setCharacterEncoding("UTF-8");
						PrintWriter writer = response.getWriter();
						writer.write("{\"status\":\"success\",\"message\":\"Login successful\"}");
						writer.flush();
					}
				}
			}
		}
	}
	
	/**
	 * 기존 방식으로 응답 전송 (폴백)
	 */
	private void sendFallbackResponse(HttpServletResponse response, CustomUser user) throws IOException {
		if (response.isCommitted()) {
			log.warn("Response already committed, cannot send fallback response");
			return;
		}
		
		try {
			// 기존 방식의 인증 결과 생성
			AuthResultDto result = makeAuthResult(user);
			
			// 기존 방식의 쿠키 설정
			cookieUtil.createAccessTokenCookie(response, result.getToken());
			
			// ApiResponse 형식으로 JSON 응답
			Map<String, Object> fallbackData = new HashMap<>();
			fallbackData.put("token", result.getToken());
			fallbackData.put("userInfo", result.getUser());
			
			ApiResponse<Map<String, Object>> apiResponse = ApiResponse.of(fallbackData, "로그인 성공");
			
			// 응답 전송
			JsonResponse.send(response, apiResponse);
			
			log.info("로그인 성공 (기존 방식): memberId={}, 쿠키 설정 완료", 
					user.getMember().getMemberId());
		} catch (Exception e) {
			log.error("Failed to send fallback response: {}", e.getMessage());
			throw new IOException("Failed to send authentication response", e);
		}
	}
}
