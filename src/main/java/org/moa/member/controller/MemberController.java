package org.moa.member.controller;

import javax.validation.Valid;

import org.moa.global.response.ApiResponse;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
