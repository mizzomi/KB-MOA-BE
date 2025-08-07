package org.moa.global.security.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
	private Long tokenId;
	private Long memberId;
	private String token;
	private String tokenFamily;  // Token Rotation을 위한 패밀리 ID
	private LocalDateTime expiryDate;
	private LocalDateTime createdAt;
	private boolean revoked;
}
