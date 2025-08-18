package org.moa.global.security.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonResponse {
	private static final ObjectMapper objectMapper;
	
	static {
		objectMapper = new ObjectMapper();
		// Java 8 Time 모듈 등록 (LocalDateTime 등 지원)
		objectMapper.registerModule(new JavaTimeModule());
		// 날짜를 타임스탬프가 아닌 문자열로 직렬화
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
	
	public static <T> void send(HttpServletResponse response, T result) throws IOException {
		// Response가 이미 커밋되었는지 확인
		if (response.isCommitted()) {
			log.warn("Response already committed. Cannot send JSON response.");
			return;
		}
		
		// Content-Type과 인코딩 설정
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		// Status가 설정되지 않았으면 200 OK로 설정
		if (response.getStatus() == 0) {
			response.setStatus(HttpServletResponse.SC_OK);
		}
		
		// JSON 문자열로 변환
		String jsonResponse = objectMapper.writeValueAsString(result);
		
		// PrintWriter를 사용하여 응답 전송
		try {
			PrintWriter writer = response.getWriter();
			writer.write(jsonResponse);
			writer.flush();
			// writer.close()를 호출하지 않음 - 컨테이너가 관리
			log.debug("JSON response sent successfully");
		} catch (IllegalStateException e) {
			// getOutputStream()이 이미 호출된 경우
			log.error("Cannot get writer, trying output stream", e);
			response.getOutputStream().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
			response.getOutputStream().flush();
		} catch (IOException e) {
			log.error("Failed to write JSON response: {}", e.getMessage());
			throw e;
		}
	}

	public static void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		// Response가 이미 커밋되었는지 확인
		if (response.isCommitted()) {
			log.warn("Response already committed. Cannot send error response.");
			return;
		}
		
		response.setStatus(status.value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		try {
			PrintWriter writer = response.getWriter();
			writer.write(message);
			writer.flush();
			// writer.close()를 호출하지 않음
		} catch (IllegalStateException e) {
			log.error("Cannot get writer for error response", e);
			response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
			response.getOutputStream().flush();
		}
	}
}
