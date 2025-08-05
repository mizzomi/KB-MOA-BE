package org.moa.global.account.controller;

import java.math.BigDecimal;

import org.moa.global.account.dto.payment.PaymentRequestDto;
import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.account.service.AccountService;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member/account")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/pay")
	public ResponseEntity<ApiResponse<PaymentResponseDto>> getAccounts(
		@AuthenticationPrincipal CustomUser customUser,
		@RequestBody PaymentRequestDto paymentRequestDto
	) {
		Long memberId = customUser.getMember().getMemberId();
		BigDecimal amount =  paymentRequestDto.getPrice();
		String paymentName = paymentRequestDto.getPaymentName();

		log.info("AccountController ==== memberId = {}", memberId);
		log.info("AccountController ==== amount = {}", amount);
		log.info("AccountController ==== paymentName = {}", paymentName);

		return ResponseEntity
			.status(StatusCode.OK.getStatus())
			.body(ApiResponse.of(accountService.makePayment(memberId, amount, paymentName)));
	}
}
