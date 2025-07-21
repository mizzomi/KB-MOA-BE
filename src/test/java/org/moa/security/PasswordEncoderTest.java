package org.moa.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.global.config.RootConfig;
import org.moa.global.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
	RootConfig.class,
	SecurityConfig.class
})
public class PasswordEncoderTest {

	@Autowired
	private PasswordEncoder pwEncoder;

	@Test
	public void testEncode() {
		String str = "1234";
		String enStr = pwEncoder.encode(str);
		log.info("password: " + enStr);
		String enStr2 = pwEncoder.encode(str);
		log.info("password: " + enStr2);
		// 암호화
		// 암호화
		log.info("match :" + pwEncoder.matches(str, enStr));
		log.info("match :" + pwEncoder.matches(str, enStr2)); // 비밀번호 일치 여부 검사
		// 비밀번호 일치 여부 검사
	}
}
