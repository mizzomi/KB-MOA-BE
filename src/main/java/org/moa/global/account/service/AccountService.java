package org.moa.global.account.service;

import java.math.BigDecimal;
import java.util.List;

import org.moa.global.account.dto.payment.LinkPaymentRecordsToTripDto;
import org.moa.global.account.dto.payment.PaymentRecordResponseDto;
import org.moa.global.account.dto.payment.PaymentRequestDto;
import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.security.domain.CustomUser;

public interface AccountService {
	boolean validateAccountNumber(String accountNumber, String accountPassword);

	PaymentResponseDto makePayment(Long memberId, BigDecimal amount, String paymentName);

	List<PaymentRecordResponseDto> getPaymentRecords(Long tripId);

	boolean LinkPaymentRecordToTrip(Long tripId,LinkPaymentRecordsToTripDto dto);
}
