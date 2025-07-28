package org.moa.global.account.service;

import java.math.BigDecimal;

import org.moa.global.account.dto.payment.PaymentRequestDto;
import org.moa.global.account.dto.payment.PaymentResponseDto;

public interface AccountService {
	boolean validateAccountNumber(String accountNumber, String accountPassword);

	PaymentResponseDto makePayment(Long memberId, BigDecimal amount, String paymentName);
}
