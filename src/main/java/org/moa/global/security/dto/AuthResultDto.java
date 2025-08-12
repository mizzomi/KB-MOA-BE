package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResultDto {
	private String accessToken;
	private String refreshToken;
	private UserInfoDto user;
	private Long accessTokenExpiresIn;  // Access Token 만료 시간 (밀리초)
	private Long refreshTokenExpiresIn; // Refresh Token 만료 시간 (밀리초)
	
	// 기존 코드와의 호환성을 위한 생성자
	public AuthResultDto(String accessToken, UserInfoDto user) {
		this.accessToken = accessToken;
		this.user = user;
	}
	
	// token getter (기존 코드 호환용)
	public String getToken() {
		return accessToken;
	}
	
	// token setter (기존 코드 호환용)
	public void setToken(String token) {
		this.accessToken = token;
	}
}
