package org.moa.global.test.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {
	private String name;
	private Integer age;
	private String phone;

	public User toEntity() {
		return User.builder()
			.name(this.name)
			.age(this.age)
			.phone(this.phone)
			.build();
	}
}
