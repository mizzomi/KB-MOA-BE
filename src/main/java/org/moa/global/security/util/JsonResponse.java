package org.moa.global.security.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonResponse {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	public static <T> void send(HttpServletResponse response, T result) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		
		// OutputStreamWriter를 사용하여 명시적으로 UTF-8 인코딩
		try (Writer out = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
			objectMapper.writeValue(out, result);
			out.flush();
		}
	}

	public static void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		
		try (Writer out = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
			out.write(message);
			out.flush();
		}
	}
}
