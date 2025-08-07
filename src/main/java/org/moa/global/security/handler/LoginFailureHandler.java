package org.moa.global.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
	
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
										AuthenticationException exception) throws IOException, ServletException {
		
		// 클라이언트 정보 추출
		String ipAddress = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		String email = request.getParameter("email");
		
		// 실패 원인 분석
		String errorMessage;
		String failureReason;
		
		if (exception instanceof BadCredentialsException) {
			errorMessage = "사용자 ID 또는 비밀번호가 일치하지 않습니다.";
			failureReason = "Invalid credentials";
		} else if (exception instanceof LockedException) {
			errorMessage = "계정이 잠겨있습니다. 관리자에게 문의하세요.";
			failureReason = "Account locked";
		} else if (exception instanceof DisabledException) {
			errorMessage = "비활성화된 계정입니다.";
			failureReason = "Account disabled";
		} else {
			errorMessage = "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.";
			failureReason = exception.getClass().getSimpleName();
		}
		
		// 로그 기록 (Redis 감사 로그로 대체 가능)
		log.warn("Login failed - Email: {}, IP: {}, Reason: {}", email, ipAddress, failureReason);
		
		// 에러 응답
		JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, errorMessage);
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
