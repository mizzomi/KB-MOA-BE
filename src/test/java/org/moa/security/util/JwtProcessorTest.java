package org.moa.security.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.global.config.RootConfig;
import org.moa.global.security.config.SecurityConfig;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.log4j.Log4j2;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Log4j2
class JwtProcessorTest {
	@Autowired
	JwtProcessor jwtProcessor;

	@Test
	void generateToken() {
		Long memberId = 3L;
		String token = jwtProcessor.generateToken(memberId);
		log.info(token);
		assertNotNull(token);
	}

	@Test
	void getMmeberId() {
		String token = "REDACTED_TEST_JWT";
		Long memberId = jwtProcessor.getMemberId(token);
		log.info(memberId.toString());
		assertNotNull(memberId);
	}

	@Test
	void validateToken() {
		// 5분 경과 후 테스트
		String token = "REDACTED_TEST_JWT";
		boolean isValid = jwtProcessor.validateToken(token); // 5분 경과 후면 예외 발생
		log.info(isValid);
		assertTrue(isValid); // 5분전이면 true
	}
}