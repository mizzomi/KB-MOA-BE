package org.moa.global.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.response.ApiResponse;
import org.moa.global.type.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {
	
	private final ObjectMapper objectMapper;  // Bean으로 주입
	
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
		StatusCode statusCode;
		
		if (exception instanceof BadCredentialsException) {
			errorMessage = "사용자 ID 또는 비밀번호가 일치하지 않습니다.";
			failureReason = "Invalid credentials";
			statusCode = StatusCode.AUTH_FAILED;
		} else if (exception instanceof LockedException) {
			errorMessage = "계정이 잠겨있습니다. 관리자에게 문의하세요.";
			failureReason = "Account locked";
			statusCode = StatusCode.FORBIDDEN;
		} else if (exception instanceof DisabledException) {
			errorMessage = "비활성화된 계정입니다.";
			failureReason = "Account disabled";
			statusCode = StatusCode.FORBIDDEN;
		} else {
			errorMessage = "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.";
			failureReason = exception.getClass().getSimpleName();
			statusCode = StatusCode.AUTH_ERROR;
		}
		
		// 로그 기록 (Redis 감사 로그로 대체 가능)
		log.warn("Login failed - Email: {}, IP: {}, Reason: {}", email, ipAddress, failureReason);
		
		// ApiResponse 형식으로 에러 응답
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json;charset=UTF-8");
		
		ApiResponse<Void> apiResponse = ApiResponse.error(statusCode, errorMessage);
		
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);
		response.getWriter().write(jsonResponse);
		response.getWriter().flush();
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
