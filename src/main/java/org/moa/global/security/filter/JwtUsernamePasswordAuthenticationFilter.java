package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.dto.LoginDto;
import org.moa.global.security.handler.LoginFailureHandler;
import org.moa.global.security.handler.LoginSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	// 스프링 생성자 주입을 통해 전달
	public JwtUsernamePasswordAuthenticationFilter(
		AuthenticationManager authenticationManager, // SecurityConfig가 생성된 이후에 등록됨
		LoginSuccessHandler loginSuccessHandler,
		LoginFailureHandler loginFailureHandler) {
		super(authenticationManager);
		setFilterProcessesUrl("/api/public/login"); // POST 로그인 요청 url - 기존 경로
		setAuthenticationSuccessHandler(loginSuccessHandler); // 로그인 성공 핸들러 등록
		setAuthenticationFailureHandler(loginFailureHandler); // 로그인 실패 핸들러 등록
		// ContinueChainBeforeSuccessfulAuthentication을 false로 설정
		setContinueChainBeforeSuccessfulAuthentication(false);
	}
	
	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		// OPTIONS 요청은 인증 불필요
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		return super.requiresAuthentication(request, response);
	}

	// 로그인 요청 URL인 경우 로그인 작업 처리
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
		throws AuthenticationException {
		
		// OPTIONS 요청은 처리하지 않음
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			log.debug("OPTIONS request detected, skipping authentication attempt");
			return null;
		}
		
		// POST 요청이 아닌 경우 처리 안함  
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			log.warn("Invalid method for login: {}", request.getMethod());
			throw new AuthenticationException("Method not supported: " + request.getMethod()) {};
		}
		
		// 요청 BODY의 JSON에서 email, password  LoginDTO
		LoginDto login = LoginDto.of(request);
		
		log.debug("Login attempt for email: {}", login.getEmail());
		
		// 인증 토큰(UsernamePasswordAuthenticationToken) 구성
		UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword());
			
		// AuthenticationManager에게 인증 요청
		return getAuthenticationManager().authenticate(authenticationToken);
	}
	
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain, Authentication authResult) throws IOException, ServletException {
		// 성공 핸들러 호출 전 로그
		log.debug("Authentication successful, calling success handler");
		
		// Response가 이미 커밋되었는지 확인
		if (response.isCommitted()) {
			log.warn("Response already committed before calling success handler");
			return;
		}
		
		// 성공 핸들러를 직접 호출
		getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
		
		// 성공 핸들러 호출 후 로그
		log.debug("Success handler completed");
		
		// 필터 체인을 계속하지 않음 (응답이 이미 전송됨)
		// chain.doFilter를 호출하지 않음
	}
	
	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		// 실패 핸들러 호출 전 로그
		log.debug("Authentication failed, calling failure handler");
		
		// Response가 이미 커밋되었는지 확인
		if (response.isCommitted()) {
			log.warn("Response already committed before calling failure handler");
			return;
		}
		
		// 실패 핸들러 직접 호출
		getFailureHandler().onAuthenticationFailure(request, response, failed);
		
		// 실패 핸들러 호출 후 로그
		log.debug("Failure handler completed");
		
		// 필터 체인을 계속하지 않음
	}
}
