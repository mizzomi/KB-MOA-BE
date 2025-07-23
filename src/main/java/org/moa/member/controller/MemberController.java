package org.moa.member.controller;

import javax.validation.Valid;

import org.moa.global.response.ApiResponse;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.dto.verify.MemberVerifyRequestDto;
import org.moa.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping("/public/verifyJoin")
	public ResponseEntity<ApiResponse<?>> verifyJoin(@Valid @RequestBody MemberVerifyRequestDto dto) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.verifyJoin(dto)));
	}

	@PostMapping("/public/join")
	public ResponseEntity<ApiResponse<?>> join(@Valid @RequestBody MemberJoinRequestDto dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(memberService.userJoin(dto)));
	}

	@GetMapping("/search-by-email")
	public ResponseEntity<ApiResponse<?>> searchUserIdByEmail(@Valid @RequestParam String email) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.searchUserIdByEmail(email)));
	}
}
