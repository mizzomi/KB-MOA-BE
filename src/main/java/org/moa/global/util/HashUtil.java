package org.moa.global.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HashUtil {

	private final BCryptPasswordEncoder encoder;

	public String hash(String plain) {
		if (plain == null) {
			throw new IllegalArgumentException("입력값이 null입니다.");
		}
		return encoder.encode(plain);
	}

	public boolean matches(String plain, String hashed) {
		if (plain == null || hashed == null) {
			return false;
		}
		return encoder.matches(plain, hashed);
	}
}
