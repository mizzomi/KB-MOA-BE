package org.moa.global.handler;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

import org.moa.global.response.ApiResponse;
import org.moa.global.type.StatusCode;
import org.moa.global.security.exception.TokenRefreshException;
import org.moa.global.exception.RecordNotFoundException;
import org.moa.global.exception.ForbiddenAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
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
	
	// 파일 업로드 예외 처리
	@ExceptionHandler(FileUploadException.class)
	protected ResponseEntity<ApiResponse<Object>> handleFileUploadException(FileUploadException ex) {
		log.error("FileUploadException occurred: {}", ex.getMessage());
		return ResponseEntity
			.status(StatusCode.BAD_REQUEST.getStatus())
			.body(ApiResponse.error(StatusCode.BAD_REQUEST, ex.getMessage()));
	}
	
	// 레코드를 찾을 수 없음
	@ExceptionHandler(RecordNotFoundException.class)
	protected ResponseEntity<ApiResponse<Object>> handleRecordNotFoundException(RecordNotFoundException ex) {
		log.warn("RecordNotFoundException: {}", ex.getMessage());
		return ResponseEntity
			.status(StatusCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(StatusCode.NOT_FOUND, ex.getMessage()));
	}
	
	// 접근 권한 없음
	@ExceptionHandler(ForbiddenAccessException.class)
	protected ResponseEntity<ApiResponse<Object>> handleForbiddenAccessException(ForbiddenAccessException ex) {
		log.warn("ForbiddenAccessException: {}", ex.getMessage());
		return ResponseEntity
			.status(StatusCode.FORBIDDEN.getStatus())
			.body(ApiResponse.error(StatusCode.FORBIDDEN, ex.getMessage()));
	}
	
	// ============ Security/JWT 관련 예외 처리 ============
	
	/**
	 * 토큰 갱신 실패
	 */
	@ExceptionHandler(TokenRefreshException.class)
	protected ResponseEntity<ApiResponse<Object>> handleTokenRefreshException(TokenRefreshException ex) {
		log.error("Token refresh failed: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED.value())
			.body(ApiResponse.error(StatusCode.UNAUTHORIZED, ex.getMessage()));
	}
	
	/**
	 * JWT 토큰 만료
	 */
	@ExceptionHandler(ExpiredJwtException.class)
	protected ResponseEntity<ApiResponse<Object>> handleExpiredJwtException(ExpiredJwtException ex) {
		log.debug("Token expired: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED.value())
			.body(ApiResponse.error(StatusCode.TOKEN_EXPIRED, "Token has expired"));
	}
	
	/**
	 * JWT 토큰 형식 오류
	 */
	@ExceptionHandler(MalformedJwtException.class)
	protected ResponseEntity<ApiResponse<Object>> handleMalformedJwtException(MalformedJwtException ex) {
		log.error("Malformed token: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST.value())
			.body(ApiResponse.error(StatusCode.INVALID_TOKEN, "Invalid token format"));
	}
	
	/**
	 * JWT 서명 오류
	 */
	@ExceptionHandler(SignatureException.class)
	protected ResponseEntity<ApiResponse<Object>> handleSignatureException(SignatureException ex) {
		log.error("Invalid token signature: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED.value())
			.body(ApiResponse.error(StatusCode.INVALID_SIGNATURE, "Invalid token signature"));
	}
	
	/**
	 * 인증 실패
	 */
	@ExceptionHandler(BadCredentialsException.class)
	protected ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
		log.warn("Authentication failed: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED.value())
			.body(ApiResponse.error(StatusCode.AUTH_FAILED, "Invalid credentials"));
	}
	
	/**
	 * 인증 관련 예외
	 */
	@ExceptionHandler(AuthenticationException.class)
	protected ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
		log.error("Authentication error: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED.value())
			.body(ApiResponse.error(StatusCode.AUTH_ERROR, "Authentication failed"));
	}
	
	/**
	 * 접근 권한 없음 (Spring Security)
	 */
	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return ResponseEntity
			.status(HttpStatus.FORBIDDEN.value())
			.body(ApiResponse.error(StatusCode.ACCESS_DENIED, "Access denied"));
	}
	
	// ============ 데이터베이스 관련 예외 처리 ============

	@ExceptionHandler(DataIntegrityViolationException.class)
	protected ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
		DataIntegrityViolationException ex) {
		log.error("DataIntegrityViolationException occurred", ex);
		return ResponseEntity
			.status(StatusCode.CONFLICT.getStatus())
			.body(ApiResponse.error(StatusCode.CONFLICT, "데이터 무결성 제약 조건에 위배되었습니다."));
	}

	@ExceptionHandler(DuplicateKeyException.class)
	protected ResponseEntity<ApiResponse<Object>> handleDuplicateKeyException(DuplicateKeyException ex) {
		log.error("DuplicateKeyException occurred", ex);
		return ResponseEntity
			.status(StatusCode.CONFLICT.getStatus())
			.body(ApiResponse.error(StatusCode.CONFLICT, "이미 존재하는 데이터입니다."));
	}

	@ExceptionHandler(SQLIntegrityConstraintViolationException.class)
	protected ResponseEntity<ApiResponse<Object>> handleSQLIntegrityException(
		SQLIntegrityConstraintViolationException ex) {
		log.error("SQLIntegrityConstraintViolationException occurred", ex);
		return ResponseEntity
			.status(StatusCode.CONFLICT.getStatus())
			.body(ApiResponse.error(StatusCode.CONFLICT, "DB 제약조건 위반입니다: " + ex.getMessage()));
	}

	// 일반 예외 처리 (맨 마지막에 위치)
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
		log.error("Unhandled exception occurred", ex);
		return ResponseEntity
			.status(StatusCode.INTERNAL_ERROR.getStatus())
			.body(ApiResponse.error(StatusCode.INTERNAL_ERROR, "An unexpected error occurred"));
	}
}
