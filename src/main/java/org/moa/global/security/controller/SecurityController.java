package org.moa.global.security.controller;

import java.time.LocalDateTime;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/api")
@RestController
public class SecurityController {

	@GetMapping("/public/all") // 모두 접근 가능
	public ResponseEntity<String> doAll() {
		log.info("do all can access everybody");
		LocalDateTime now = LocalDateTime.now();
		return ResponseEntity.ok("all can access everybody, now is " + now);
	}

	@GetMapping("/member")
	public ResponseEntity<String> doMember(Authentication authentication) {
		UserDetails userDetails = (UserDetails)authentication.getPrincipal();
		log.info("username = " + userDetails.getUsername());
		return ResponseEntity.ok(userDetails.getUsername());
	}

	@GetMapping("/admin")
	public ResponseEntity<Member> doAdmin(@AuthenticationPrincipal CustomUser customUser) {
		Member member = customUser.getMember();
		log.info("username = " + member);
		return ResponseEntity.ok(member);
	}

	@GetMapping("/member/findById")
	public ResponseEntity<ApiResponse<Member>> findMember(Authentication authentication) {
		CustomUser customuser = (CustomUser)authentication.getPrincipal();
		Member member = customuser.getMember();

		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(member));
	}
}
