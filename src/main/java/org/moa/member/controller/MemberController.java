package org.moa.member.controller;

import javax.validation.Valid;

import org.moa.global.response.ApiResponse;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping("/join")
	public ResponseEntity<ApiResponse<?>> join(@Valid @RequestBody MemberJoinRequestDto dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(memberService.userJoin(dto)));
	}

	@GetMapping("/search-by-email")
	public ResponseEntity<ApiResponse<?>> searchUserIdByEmail(@Valid @RequestParam String email){
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(memberService.searchUserIdByEmail(email)));
	}
}
