package org.moa.global.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.dto.AuthResultDto;
import org.moa.global.security.dto.UserInfoDto;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JsonResponse;
import org.moa.global.security.util.JwtProcessor;
import org.moa.global.util.CookieUtil;
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
	private final CookieUtil cookieUtil;
	private final RedisTokenService redisTokenService;
	
	@Value("${server.env:local}")
	private String serverEnv;

	private AuthResultDto makeAuthResult(CustomUser user) {
		Long memberId = user.getMember().getMemberId();
		
		// Access Token 생성
		String accessToken = jwtProcessor.generateAccessToken(memberId);
		
		// Refresh Token Rotation을 위한 Family ID 생성
		String familyId = redisTokenService.generateFamilyId();
		
		// Refresh Token 생성
		String refreshToken = jwtProcessor.generateRefreshToken(memberId, familyId);
		
		// Redis에 Refresh Token 저장 (RTR을 위한 Family ID와 함께)
		redisTokenService.saveRefreshToken(memberId, refreshToken, familyId);
		
		log.info("토큰 생성 완료 - memberId: {}, familyId: {}", memberId, familyId);
		
		// 결과 DTO 생성
		return AuthResultDto.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)  // 내부 처리용, 응답에서는 제외
			.user(UserInfoDto.of(user.getMember()))
			.accessTokenExpiresIn(jwtProcessor.getAccessTokenValidTime())
			.refreshTokenExpiresIn(jwtProcessor.getRefreshTokenValidTime())
			.build();
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		
		// 인증 결과 Principal
		CustomUser user = (CustomUser)authentication.getPrincipal();
		
		// 인증 성공 결과 생성
		AuthResultDto result = makeAuthResult(user);
		
		// 1. Refresh Token을 HttpOnly 쿠키에 저장 (보안 강화)
		// 프론트엔드에서 접근 불가, 자동으로 서버에 전송됨
		cookieUtil.createRefreshTokenCookie(response, result.getRefreshToken());
		
		// 2. Access Token은 응답 본문으로만 전달
		// 프론트엔드가 읽어서 localStorage에 저장 후 Authorization 헤더로 사용
		AuthResultDto responseDto = AuthResultDto.builder()
			.accessToken(result.getAccessToken())
			.refreshToken(null)  // 보안상 응답 본문에서 제외
			.user(result.getUser())
			.accessTokenExpiresIn(result.getAccessTokenExpiresIn())
			.refreshTokenExpiresIn(result.getRefreshTokenExpiresIn())
			.build();
		
		// JSON 응답
		JsonResponse.send(response, responseDto);
		
		log.info("로그인 성공 - memberId: {}, 환경: {}, AT: Response Body, RT: HttpOnly Cookie", 
			user.getMember().getMemberId(), serverEnv);
	}
}
