package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 * RefreshToken은 Cookie로 전송되므로 응답에 포함하지 않음
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
	private String accessToken;
	private UserInfoDto user;
	private Long expiresIn;  // Access Token 만료 시간 (초)
}
