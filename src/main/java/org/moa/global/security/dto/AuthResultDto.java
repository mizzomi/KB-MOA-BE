package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResultDto {
	private String accessToken;
	private String refreshToken;  // Cookie로 전송될 예정
	private UserInfoDto user;
	private Long expiresIn;  // Access Token 만료 시간 (초)
}
