package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.service.CustomUserDetailsService;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JwtProcessor;
import org.moa.global.util.CookieUtil;
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
	private final RedisTokenService redisTokenService;
	private final CookieUtil cookieUtil;

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
		
		String token = null;
		
		// 1. Authorization 헤더에서 Access Token 추출
		// 프론트엔드가 localStorage에서 읽어서 보낸 토큰
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			token = bearerToken.substring(BEARER_PREFIX.length());
		}
		
		// 2. 레거시 JWT_TOKEN 쿠키 확인
		if (token == null) {
			String legacyToken = cookieUtil.getCookieValue(request, "JWT_TOKEN");
			if (legacyToken != null) {
				token = legacyToken;
				log.debug("레거시 JWT_TOKEN 쿠키에서 토큰 추출");
			}
		}
		
		// 3. 토큰이 있으면 검증 및 인증 처리
		if (token != null) {
			try {
				// 블랙리스트 체크
				if (redisTokenService.isBlacklisted(token)) {
					log.warn("블랙리스트에 있는 토큰 사용 시도");
					filterChain.doFilter(request, response);
					return;
				}
				
				// 토큰 유효성 검증
				if (jwtProcessor.validateToken(token)) {
					// Access Token 타입 확인 (type claim이 있는 경우만)
					try {
						String tokenType = jwtProcessor.getTokenType(token);
						if (tokenType != null && !"ACCESS".equals(tokenType)) {
							log.warn("Access Token이 아닌 토큰 사용 시도: {}", tokenType);
							filterChain.doFilter(request, response);
							return;
						}
					} catch (Exception e) {
						// type claim이 없는 경우 (레거시 토큰) - 통과 허용
						log.debug("토큰 타입 정보 없음 (레거시 토큰)");
					}
					
					// 인증 정보 설정
					Authentication authentication = getAuthentication(token);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					
					Long memberId = jwtProcessor.getMemberId(token);
					log.debug("인증 성공 - memberId: {}", memberId);
				} else {
					log.warn("유효하지 않은 토큰");
				}
			} catch (Exception e) {
				log.error("토큰 처리 중 오류 발생: {}", e.getMessage());
			}
		}
		
		filterChain.doFilter(request, response);
	}
}
