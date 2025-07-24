package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.service.CustomUserDetailsService;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer "; // 끝에 공백 있음
	private final JwtProcessor jwtProcessor;
	private final UserDetailsService userDetailsService;

	private Authentication getAuthentication(String token) {
		Long memberId = jwtProcessor.getMemberId(token);
		UserDetails principal = ((CustomUserDetailsService)userDetailsService).loadUserByMemberId(memberId);
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			String token = bearerToken.substring(BEARER_PREFIX.length());
			// 토큰에서 사용자 정보 추출 및 Authentication 객체 구성 후 SecurityContext에 저장
			Authentication authentication = getAuthentication(token);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		super.doFilter(request, response, filterChain);
	}
}
