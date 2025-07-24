package org.moa.global.security.dto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.BadCredentialsException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
	private String email;
	private String password;

	public static LoginDto of(HttpServletRequest request) {
		ObjectMapper om = new ObjectMapper();
		try {
			String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
				.lines()
				.collect(Collectors.joining("\n"));

			log.info("LoginDto body 수신: {}", body); // 로그 찍히는지 확인

			return om.readValue(body, LoginDto.class);
		} catch (Exception e) {
			log.error("LoginDto 파싱 실패", e);
			throw new BadCredentialsException("해당 email 또는 password가 없습니다.");
		}
	}
}
