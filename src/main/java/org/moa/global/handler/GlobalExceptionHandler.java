package org.moa.global.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.moa.global.response.ApiResponse;
import org.moa.global.type.StatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	//JSON 파싱 / 타입 변환 실패 시
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {

		//상세 에러는 로그
		log.error("Message not readable exeption occurred", ex);

		//클라이언트에는 최소한의 정보
		return ResponseEntity
			.status(StatusCode.BAD_REQUEST.getStatus())
			.body(ApiResponse.error(StatusCode.BAD_REQUEST, "Invalid request format"));
	}

	//@Valid 미충족시
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		log.error("Method argument not valid exeption occurred", ex);

		List<ApiResponse.FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> ApiResponse.FieldError.builder()
				.field(error.getField())
				.reason(error.getDefaultMessage())
				.build())
			.collect(Collectors.toList());

		return ResponseEntity
			.status(StatusCode.INVALID_INPUT.getStatus())
			.body(ApiResponse.error(StatusCode.INVALID_INPUT, errors));
	}

	// 비즈니스 예외 처리
	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
		log.error("BusinessException caught in GlobalExceptionHandler: {}", ex.getMessage());
		return ResponseEntity
			.status(ex.getStatusCode().getStatus())
			.body(ApiResponse.error(ex.getStatusCode(), ex.getMessage()));
	}

	// 일반 예외 처리
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
		log.error("Unhandled exception occurred", ex);
		return ResponseEntity
			.status(StatusCode.INTERNAL_ERROR.getStatus())
			.body(ApiResponse.error(StatusCode.INTERNAL_ERROR, ex.getMessage()));
	}
}
