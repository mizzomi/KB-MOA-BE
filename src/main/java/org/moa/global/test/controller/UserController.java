package org.moa.global.test.controller;

import org.moa.global.response.ApiResponse;
import org.moa.global.test.domain.UserRequestDto;
import org.moa.global.test.domain.UserResponseDto;
import org.moa.global.test.service.UserService;
import org.moa.global.type.StatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
	private final UserService userService;

	// 기존 방식
	// @GetMapping
	// public ResponseEntity<UserResponseDto> getUser(String name) {
	// 	return ResponseEntity.ok().body(userService.getUser(name));
	// }

	//ApiResponse 적용후
	@GetMapping
	public ResponseEntity<ApiResponse<UserResponseDto>> getUser(@RequestParam String name) {
		return ResponseEntity
			.status(StatusCode.OK.getStatus())
			.body(ApiResponse.of(userService.getUser(name)));
	}

	// @PostMapping
	// public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto userRequestDto) {
	// 	return ResponseEntity.ok().body(userService.createUser(userRequestDto));
	// }

	@PostMapping
	public ResponseEntity<ApiResponse<UserResponseDto>> createUser(@RequestBody UserRequestDto user) {
		return ResponseEntity
			.status(StatusCode.OK.getStatus())
			.body(ApiResponse.of(userService.createUser(user)));
	}
}
