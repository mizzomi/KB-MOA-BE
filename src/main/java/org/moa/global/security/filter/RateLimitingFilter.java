package org.moa.global.security.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
	
	@Value("${rate.limit.requests-per-second:10}")
	private double requestsPerSecond;
	
	@Value("${rate.limit.burst-capacity:20}")
	private int burstCapacity;
	
	// IP별 Rate Limiter 캐시
	private final LoadingCache<String, RateLimiter> limiters;
	
	// 차단된 IP 목록
	private final ConcurrentHashMap<String, Long> blockedIps = new ConcurrentHashMap<>();
	
	public RateLimitingFilter() {
		// 5분 후 자동 만료되는 캐시
		this.limiters = CacheBuilder.newBuilder()
				.maximumSize(10000)
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.build(new CacheLoader<String, RateLimiter>() {
					@Override
					public RateLimiter load(String key) {
						return RateLimiter.create(requestsPerSecond);
					}
				});
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
									FilterChain filterChain) throws ServletException, IOException {
		
		String clientIp = getClientIp(request);
		String requestUri = request.getRequestURI();
		
		// 정적 리소스는 제한하지 않음
		if (isStaticResource(requestUri)) {
			filterChain.doFilter(request, response);
			return;
		}
		
		// 차단된 IP 확인
		if (isBlocked(clientIp)) {
			log.warn("Blocked IP attempted access: {}", clientIp);
			sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, 
					"Your IP has been temporarily blocked due to excessive requests");
			return;
		}
		
		try {
			RateLimiter rateLimiter = limiters.get(clientIp);
			
			// 로그인 엔드포인트는 더 엄격한 제한
			if (requestUri.contains("/login")) {
				rateLimiter = getLoginRateLimiter(clientIp);
			}
			
			// Rate limit 체크
			if (!rateLimiter.tryAcquire()) {
				handleRateLimitExceeded(clientIp, requestUri);
				
				// 응답 헤더에 Rate Limit 정보 추가
				response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerSecond));
				response.setHeader("X-RateLimit-Remaining", "0");
				response.setHeader("X-RateLimit-Retry-After", "1");
				
				sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, 
						"Rate limit exceeded. Please try again later.");
				return;
			}
			
			// 성공시 Rate Limit 정보 헤더 추가
			response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerSecond));
			
		} catch (Exception e) {
			log.error("Rate limiting error for IP: {}", clientIp, e);
		}
		
		filterChain.doFilter(request, response);
	}
	
	/**
	 * 로그인 엔드포인트용 별도 Rate Limiter
	 */
	private RateLimiter getLoginRateLimiter(String clientIp) {
		// 로그인은 분당 5회로 제한
		return RateLimiter.create(5.0 / 60.0);
	}
	
	/**
	 * Rate Limit 초과 처리
	 */
	private void handleRateLimitExceeded(String clientIp, String requestUri) {
		log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
		
		// 연속 초과시 일시 차단
		Integer violations = rateLimitViolations.compute(clientIp, (k, v) -> v == null ? 1 : v + 1);
		
		if (violations >= 10) {
			// 10회 이상 초과시 5분간 차단
			blockIp(clientIp, Duration.ofMinutes(5));
			rateLimitViolations.remove(clientIp);
		}
	}
	
	private final ConcurrentHashMap<String, Integer> rateLimitViolations = new ConcurrentHashMap<>();
	
	/**
	 * IP 차단
	 */
	private void blockIp(String ip, Duration duration) {
		long unblockTime = System.currentTimeMillis() + duration.toMillis();
		blockedIps.put(ip, unblockTime);
		log.warn("IP {} has been blocked until {}", ip, new java.util.Date(unblockTime));
	}
	
	/**
	 * IP 차단 여부 확인
	 */
	private boolean isBlocked(String ip) {
		Long unblockTime = blockedIps.get(ip);
		if (unblockTime == null) {
			return false;
		}
		
		if (System.currentTimeMillis() > unblockTime) {
			// 차단 해제
			blockedIps.remove(ip);
			return false;
		}
		
		return true;
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
				// 첫 번째 IP 추출 (프록시 체인의 경우)
				return ip.split(",")[0].trim();
			}
		}
		
		return request.getRemoteAddr();
	}
	
	/**
	 * 정적 리소스 확인
	 */
	private boolean isStaticResource(String uri) {
		return uri.contains("/assets/") || 
			   uri.contains("/static/") || 
			   uri.endsWith(".css") || 
			   uri.endsWith(".js") || 
			   uri.endsWith(".png") || 
			   uri.endsWith(".jpg");
	}
	
	/**
	 * 에러 응답 전송
	 */
	private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
			throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/json");
		response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
	}
}
