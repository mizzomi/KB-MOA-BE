package org.moa.global.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.domain.CustomUser;
import org.moa.global.security.dto.AuthResultDto;
import org.moa.global.security.dto.UserInfoDto;
import org.moa.global.security.util.JsonResponse;
import org.moa.global.security.util.JwtProcessor;
import org.moa.global.util.CookieUtil;
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

	private AuthResultDto makeAuthResult(CustomUser user) {
		Long memberId = user.getMember().getMemberId(); // member 엔티티에서 ID 꺼냄
		String token = jwtProcessor.generateToken(memberId); // Long → String 변환은 내부에서 처리
		return new AuthResultDto(token, UserInfoDto.of(user.getMember()));
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		// 인증 결과 Principal
		CustomUser user = (CustomUser)authentication.getPrincipal();
		// 인증 성공 결과를 JSON으로 직접 응답
		AuthResultDto result = makeAuthResult(user);
		
		// JWT를 쿠키에도 저장 (환경에 따라 자동으로 Secure, SameSite 설정)
		cookieUtil.createAccessTokenCookie(response, result.getToken());
		
		// JSON 응답 (기존 방식 유지 - 프론트엔드가 선택적으로 사용)
		JsonResponse.send(response, result);
		
		log.info("로그인 성공: memberId={}, 쿠키 설정 완료", user.getMember().getMemberId());
	}
}
