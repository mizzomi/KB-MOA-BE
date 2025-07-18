package org.moa.global.test.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
	private final String name;
	private final Integer age;

	public static UserResponseDto of(User user) {
		return UserResponseDto.builder()
			.name(user.getName())
			.age(user.getAge())
			.build();
	}
}
