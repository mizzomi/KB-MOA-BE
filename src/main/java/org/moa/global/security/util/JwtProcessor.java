package org.moa.global.security.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProcessor {
	static private final long TOKEN_VALID_MILISECOND = 1000L * 60 * 60 * 365; // 1년
	private final String secretKey = "REDACTED_SECRET";
	private final Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

	// private Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); -- 운영시 사용
	// JWT 생성
	public String generateToken(Long memberId) {
		return Jwts.builder()
			.setSubject(String.valueOf(memberId))  // memberId를 문자열로 변환하여 sub에 저장
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALID_MILISECOND))
			.signWith(key)
			.compact();
	}

	public Long getMemberId(String token) {
		String subject = Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.getSubject(); // 이게 memberId 문자열임
		return Long.parseLong(subject);
	}

	// JWT 검증(유효 기간 검증) - 해석 불가인 경우 예외 발생
	public boolean validateToken(String token) {
		Jws<Claims> claims = Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token);
		return true;
	}
}
