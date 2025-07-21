package org.moa.global.security.dto;

import org.moa.member.entity.Member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
	String username;
	String email;
	String role;

	public static UserInfoDto of(Member member) {
		return new UserInfoDto(
			member.getName(),
			member.getEmail(),
			member.getMemberType().name()
		);
	}
}
