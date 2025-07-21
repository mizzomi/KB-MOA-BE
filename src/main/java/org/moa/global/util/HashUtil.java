package org.moa.global.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashUtil {
	private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public static String hash(String plain) {
		return encoder.encode(plain);
	}

	public static boolean matches(String plain, String hashed) {
		return encoder.matches(plain, hashed);
	}
}
