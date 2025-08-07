package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.service.CustomUserDetailsService;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";
	
	private final JwtProcessor jwtProcessor;
	private final UserDetailsService userDetailsService;
	private final RedisTokenService redisTokenService;

	private Authentication getAuthentication(String token) {
		Long memberId = jwtProcessor.getMemberId(token);
		UserDetails principal = ((CustomUserDetailsService) userDetailsService).loadUserByMemberId(memberId);
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// OPTIONS 요청 처리
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}
		
		// 로그인, 리프레시 엔드포인트는 토큰 검증 스킵
		String requestURI = request.getRequestURI();
		if (requestURI.equals("/api/public/login") || requestURI.equals("/api/public/auth/refresh")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			String token = bearerToken.substring(BEARER_PREFIX.length());
			
			try {
				// 토큰 검증
				if (jwtProcessor.validateToken(token)) {
					// ⭐ Redis 블랙리스트 체크 추가
					if (redisTokenService.isAccessTokenBlacklisted(token)) {
						log.warn("Blacklisted access token usage attempt");
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.getWriter().write("{\"error\":\"Token has been revoked\"}");
						return;
					}
					
					// 토큰이 Access Token인지 확인
					String tokenType = jwtProcessor.getTokenType(token);
					if (!"access".equals(tokenType)) {
						log.warn("Invalid token type: {}", tokenType);
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.getWriter().write("{\"error\":\"Invalid token type\"}");
						return;
					}
					
					// 인증 정보 설정
					Authentication authentication = getAuthentication(token);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.debug("Security Context에 인증 정보 설정 완료");
				}
			} catch (ExpiredJwtException e) {
				// Access Token이 만료된 경우
				log.warn("Access Token 만료: {}", e.getMessage());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader("Token-Expired", "true");
				response.getWriter().write("{\"error\":\"Access token expired\", \"code\":\"TOKEN_EXPIRED\"}");
				return;
			} catch (Exception e) {
				// 그 외 토큰 검증 실패
				log.error("Token 검증 실패: {}", e.getMessage());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().write("{\"error\":\"Invalid token\"}");
				return;
			}
		}
		
		filterChain.doFilter(request, response);
	}
}
