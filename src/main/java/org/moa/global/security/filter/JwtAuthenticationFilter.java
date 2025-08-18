package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import org.moa.global.security.service.CustomUserDetailsService;
import org.moa.global.security.service.JwtTokenService;
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

/**
 * JWT 인증 필터
 * Access Token 검증 및 블랙리스트 체크
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer "; // 끝에 공백 있음
	
	private final JwtProcessor jwtProcessor;
	private final JwtTokenService jwtTokenService;
	private final UserDetailsService userDetailsService;

	/**
	 * Token으로부터 Authentication 객체 생성
	 */
	private Authentication getAuthentication(String token) {
		try {
			// 새로운 JwtTokenService 사용
			Long memberId = jwtTokenService.getMemberId(token);
			UserDetails principal = ((CustomUserDetailsService)userDetailsService).loadUserByMemberId(memberId);
			return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		} catch (Exception e) {
			log.error("Failed to create authentication from token: {}", e.getMessage());
			// 기존 JwtProcessor로 폴백
			try {
				Long memberId = jwtProcessor.getMemberId(token);
				UserDetails principal = ((CustomUserDetailsService)userDetailsService).loadUserByMemberId(memberId);
				return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			} catch (Exception fallbackError) {
				log.error("Fallback authentication also failed: {}", fallbackError.getMessage());
				return null;
			}
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// OPTIONS 요청은 통과
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		// 인증 제외 경로 체크
		String requestURI = request.getRequestURI();
		if (isExcludedPath(requestURI)) {
			filterChain.doFilter(request, response);
			return;
		}
		
		// 로그인 경로는 JWT 필터 통과 (로그인 필터에서 처리)
		if ("/api/public/login".equals(requestURI)) {
			log.debug("Login path detected, skipping JWT authentication");
			filterChain.doFilter(request, response);
			return;
		}

		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			String token = bearerToken.substring(BEARER_PREFIX.length());
			
			try {
				// 1. 블랙리스트 체크
				if (jwtTokenService.isBlacklisted(token)) {
					log.warn("Blacklisted token attempted to access: {}", requestURI);
					if (!response.isCommitted()) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("application/json");
						response.getWriter().write("{\"error\":\"Token has been blacklisted\"}");
					}
					return;
				}

				// 2. 토큰 유효성 검증
				Claims claims = jwtTokenService.validateToken(token);
				
				// 3. Authentication 객체 생성 및 SecurityContext에 저장
				Authentication authentication = getAuthentication(token);
				if (authentication != null) {
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.debug("Authentication set for user: {}", authentication.getName());
				} else {
					log.warn("Failed to create authentication for token");
					if (!response.isCommitted()) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("application/json");
						response.getWriter().write("{\"error\":\"Invalid authentication\"}");
					}
					return;
				}
				
			} catch (Exception e) {
				log.error("JWT authentication failed: {}", e.getMessage());
				// 토큰이 유효하지 않은 경우
				SecurityContextHolder.clearContext();
				if (!response.isCommitted()) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
				}
				return;
			}
		}
		
		filterChain.doFilter(request, response);
	}

	/**
	 * 인증 제외 경로 확인
	 */
	private boolean isExcludedPath(String requestURI) {
		// 인증이 필요없는 경로들
		return requestURI.startsWith("/api/public/") ||  // 모든 public 경로 (login, join, verifyJoin 등)
			   requestURI.startsWith("/static/") ||
			   requestURI.startsWith("/css/") ||
			   requestURI.startsWith("/js/") ||
			   requestURI.startsWith("/images/") ||
			   requestURI.equals("/") ||
			   requestURI.startsWith("/favicon");
	}
}
