package org.moa.global.test.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
	private Long id;
	private String name;
	private Integer age;
	private String phone;

}
