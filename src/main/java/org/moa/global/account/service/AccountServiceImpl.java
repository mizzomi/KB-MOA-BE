package org.moa.global.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.account.entity.Account;
import org.moa.global.account.entity.PaymentRecord;
import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.account.mapper.PaymentRecordMapper;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private final AccountMapper accountMapper;
	private final PaymentRecordMapper paymentRecordMapper;

	@Override
	public boolean validateAccountNumber(String accountNumber, String accountPassword) {
		return accountMapper.existsByAccountNumberAndAccountPassword(accountNumber, accountPassword);
	}

	@Override
	@Transactional
	public PaymentResponseDto makePayment(Long memberId, BigDecimal amount, String paymentName) {
		// 계좌 유효성 확인
		Account account = accountMapper.searchAccountByMemberId(memberId);
		log.info("AccountService => makePayment ==== 계좌 조회 결과: {}", account);

		if(account == null) {
			throw new BusinessException(StatusCode.ACCOUNT_NOT_FOUND);
		}

		if (account.getBalance() == null) {
			throw new BusinessException(StatusCode.INSUFFICIENT_BALANCE);
		}

		// 잔액 확인
		if(account.getBalance().compareTo(amount) < 0) {
			throw new BusinessException(StatusCode.INSUFFICIENT_BALANCE);
		}

		// 출금 처리
		BigDecimal newBalance = account.getBalance().subtract(amount);
		log.info("AccountService => makePayment ==== newBalance: {}", newBalance);
		accountMapper.withdraw(account.getAccountNumber(), amount);

		// 결제 기록 저장
		PaymentRecord paymentRecord = PaymentRecord.builder()
			.accountId(account.getAccountId())
			.memberId(account.getMemberId())
			.paymentName(paymentName)
			.paymentPrice(amount)
			.paymentDate(LocalDateTime.now())
			.paymentLocation("온라인")
			.build();
		paymentRecordMapper.insert(paymentRecord);

		// 응답
		return PaymentResponseDto.builder()
			.accountNumber(account.getAccountNumber())
			.amount(amount)
			.balance(newBalance)
			.build();
	}
}
