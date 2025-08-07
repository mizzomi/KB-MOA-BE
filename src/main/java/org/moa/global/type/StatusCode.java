package org.moa.global.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusCode {
	// Success
	OK(200, "S200", "Success"),
	CREATED(201, "S201", "Created Success"),

	// Client Errors
	BAD_REQUEST(400, "C400", "Bad Request"),
	INVALID_VERIFICATION_CODE(400, "C400", "Invalid Verification Code"),
	UNAUTHORIZED(401, "C401", "Unauthorized"),
	FORBIDDEN(403, "C403", "Forbidden"),
	NOT_FOUND(404, "C404", "Not Found"),
	CONFLICT(409, "B409", "Conflict"),
	
	// Authentication & Token Errors
	TOKEN_EXPIRED(401, "A401", "Token Expired"),
	INVALID_TOKEN(401, "A402", "Invalid Token"),
	INVALID_SIGNATURE(401, "A403", "Invalid Token Signature"),
	AUTH_FAILED(401, "A404", "Authentication Failed"),
	AUTH_ERROR(401, "A405", "Authentication Error"),
	ACCESS_DENIED(403, "A406", "Access Denied"),

	// Business Errors,
	INVALID_INPUT(400, "B400", "Invalid Input Data"),
	DUPLICATE_ENTRY(400, "B401", "Duplicate Entry"),
	DUPLICATE_REQUEST(429, "B429", "Duplicate Request"),
	BUSINESS_ERROR(400, "B430", "Business Logic Error"),
	VALIDATION_ERROR(400, "B431", "Validation Error"),

	INSUFFICIENT_BALANCE(400, "B402", "잔액이 부족합니다."),
	ACCOUNT_NOT_FOUND(404, "B404", "등록된 계좌가 없습니다."),

	// Server Errors
	INTERNAL_ERROR(500, "E500", "Internal Server Error"),
	INTERNAL_SERVER_ERROR(500, "E501", "Internal Server Error"),
	SERVICE_UNAVAILABLE(503, "E503", "Service Unavailable");

	private final int status;
	private final String code;
	private final String message;

}
