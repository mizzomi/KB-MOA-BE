package org.moa.global.security.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProcessor {
	
	// Access Token: 15분
	@Value("${jwt.access.expiration:15}")
	private int accessTokenExpirationMinutes;
	
	// Refresh Token은 RefreshTokenService에서 관리
	
	// 시크릿 키 (운영환경에서는 환경변수로 관리)
	@Value("${jwt.secret}")
	private String secretKey;
	
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Access Token 생성
	 */
	public String generateAccessToken(Long memberId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("type", "access");
		
		return Jwts.builder()
			.setClaims(claims)
			.setSubject(String.valueOf(memberId))
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + (accessTokenExpirationMinutes * 60 * 1000L)))
			.signWith(getSigningKey())
			.compact();
	}
	
	/**
	 * Token에서 memberId 추출
	 */
	public Long getMemberId(String token) {
		try {
			String subject = Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
			return Long.parseLong(subject);
		} catch (JwtException | NumberFormatException e) {
			log.error("Failed to get memberId from token", e);
			throw new JwtException("Invalid token");
		}
	}
	
	/**
	 * Token 검증
	 */
	public boolean validateToken(String token) {
		try {
			Jws<Claims> claims = Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("Token has expired: {}", e.getMessage());
			throw e;  // 만료된 토큰은 별도 처리를 위해 예외 전파
		} catch (JwtException e) {
			log.error("Token validation failed: {}", e.getMessage());
			return false;
		}
	}
	
	/**
	 * Token 만료 여부 확인
	 */
	public boolean isTokenExpired(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
			return claims.getExpiration().before(new Date());
		} catch (ExpiredJwtException e) {
			return true;
		} catch (JwtException e) {
			log.error("Failed to check token expiration", e);
			return true;
		}
	}
	
	/**
	 * Token에서 Claims 추출
	 */
	public Claims getClaims(String token) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (JwtException e) {
			log.error("Failed to extract claims from token", e);
			throw new JwtException("Invalid token");
		}
	}
	
	/**
	 * Token 타입 확인 (access/refresh 구분용)
	 */
	public String getTokenType(String token) {
		try {
			Claims claims = getClaims(token);
			return claims.get("type", String.class);
		} catch (Exception e) {
			return null;
		}
	}
}
