package org.moa.global.security.dto;

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
			return om.readValue(request.getInputStream(), LoginDto.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BadCredentialsException("해당 email 또는 password가 없습니다.");
		}
	}
}
