package org.moa.global.security.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProcessor {
	// Token 유효 시간 설정
	static private final long ACCESS_TOKEN_VALID_MILISECOND = 1000L * 60 * 60 * 24 * 7; // 15분
	static private final long REFRESH_TOKEN_VALID_MILISECOND = 1000L * 60 * 60 * 24 * 30; // 7일
	
	private final String secretKey = "REDACTED_SECRET";
	private final Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

	// Access Token 생성
	public String generateAccessToken(Long memberId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + ACCESS_TOKEN_VALID_MILISECOND);
		
		return Jwts.builder()
			.setSubject(String.valueOf(memberId))
			.setIssuedAt(now)
			.setExpiration(expiry)
			.claim("type", "ACCESS")
			.signWith(key)
			.compact();
	}
	
	// Refresh Token 생성
	public String generateRefreshToken(Long memberId, String familyId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + REFRESH_TOKEN_VALID_MILISECOND);
		
		return Jwts.builder()
			.setSubject(String.valueOf(memberId))
			.setIssuedAt(now)
			.setExpiration(expiry)
			.claim("type", "REFRESH")
			.claim("familyId", familyId)  // RTR을 위한 Family ID
			.signWith(key)
			.compact();
	}

	// Token에서 memberId 추출
	public Long getMemberId(String token) {
		String subject = getClaims(token).getSubject();
		return Long.parseLong(subject);
	}
	
	// Token에서 Family ID 추출 (Refresh Token용)
	public String getFamilyId(String token) {
		Claims claims = getClaims(token);
		return (String) claims.get("familyId");
	}
	
	// Token 타입 확인
	public String getTokenType(String token) {
		Claims claims = getClaims(token);
		return (String) claims.get("type");
	}
	
	// Token Claims 추출
	private Claims getClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	// JWT 검증(유효 기간 검증) - 해석 불가인 경우 예외 발생
	public boolean validateToken(String token) {
		try {
			Jws<Claims> claims = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
			
			// 만료 시간 체크
			Date expiration = claims.getBody().getExpiration();
			if (expiration.before(new Date())) {
				log.warn("Token 만료됨");
				return false;
			}
			
			return true;
		} catch (Exception e) {
			log.error("Token 검증 실패: {}", e.getMessage());
			return false;
		}
	}
	
	// Token 만료 시간 확인
	public Date getExpiration(String token) {
		return getClaims(token).getExpiration();
	}
	
	// Token이 만료되었는지 확인
	public boolean isTokenExpired(String token) {
		try {
			Date expiration = getExpiration(token);
			return expiration.before(new Date());
		} catch (Exception e) {
			return true;
		}
	}
	
	// Access Token 유효 시간 (밀리초)
	public long getAccessTokenValidTime() {
		return ACCESS_TOKEN_VALID_MILISECOND;
	}
	
	// Refresh Token 유효 시간 (밀리초)
	public long getRefreshTokenValidTime() {
		return REFRESH_TOKEN_VALID_MILISECOND;
	}
}
