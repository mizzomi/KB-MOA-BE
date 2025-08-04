package org.moa.global.account.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.moa.global.account.dto.payment.LinkPaymentRecordsToTripDto;
import org.moa.global.account.dto.payment.PaymentRecordResponseDto;
import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.account.entity.Account;
import org.moa.global.account.entity.PaymentRecord;
import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.account.mapper.PaymentRecordMapper;
import org.moa.global.handler.BusinessException;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.entity.TripDay;
import org.moa.trip.mapper.TripMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
	private final MemberMapper memberMapper;
	private final PaymentRecordMapper paymentRecordMapper;
	private final TripMapper tripMapper;

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

	@Override
	@Transactional
	public List<PaymentRecordResponseDto> getPaymentRecords(Long tripId){
		log.info("tripId : {}", tripId);

		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Member member = memberMapper.getByEmail(userDetails.getUsername());

		List<LocalDateTime> tripDays = tripMapper.searchDayByTripId(tripId);

		if(tripDays == null || tripDays.isEmpty()) {
			log.info("비어있음");
			return new ArrayList<>();
		}

		for(LocalDateTime tripDay : tripDays){
			log.info("day : {}", tripDay);
		}

		List<PaymentRecord> paymentRecords = paymentRecordMapper.searchByPaymentDates(tripDays, member.getMemberId());
		for(PaymentRecord paymentRecord : paymentRecords){
			log.info("paymentRecord paymentDate : {}", paymentRecord.getPaymentDate());
		}
		List<PaymentRecordResponseDto> paymentRecordResponseDtos = new ArrayList<>();

		for(PaymentRecord paymentRecord : paymentRecords){
			PaymentRecordResponseDto paymentRecordResponseDto = PaymentRecordResponseDto.builder()
					.paymentName(paymentRecord.getPaymentName())
					.paymentDate(paymentRecord.getPaymentDate())
					.paymentPrice(paymentRecord.getPaymentPrice())
					.build();
			paymentRecordResponseDtos.add(paymentRecordResponseDto);
		}

		return paymentRecordResponseDtos;
	}

	@Override
	@Transactional
	public boolean LinkPaymentRecordToTrip(Long tripId, LinkPaymentRecordsToTripDto dto) {
		// 1. 현재 로그인한 사용자 정보 조회
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Member member = memberMapper.getByEmail(userDetails.getUsername());

		// 2. 사용자가 연동을 요청한 PaymentRecord 리스트 조회
		List<PaymentRecord> paymentRecords = paymentRecordMapper.searchByIdsAndMemberId(dto.getRecordIds(), member.getMemberId());

		// 3. tripId에 해당하는 모든 TripDay 정보를 미리 조회하여 Map에 저장 (효율성 개선)
		List<TripDay> tripDays = tripMapper.searchByTripId(tripId);
		if (tripDays == null || tripDays.isEmpty()) {
			log.warn("해당 tripId({})에 대한 TripDay 정보가 존재하지 않습니다.", tripId);
			return false;
		}

		Map<LocalDate, Long> tripDayMap = tripDays.stream()
				.collect(Collectors.toMap(TripDay::getDay, TripDay::getTripDayId));

		// 4. PaymentRecord에 trip_day_id 연결 (업데이트)
		int updatedCount = 0;
		for (PaymentRecord paymentRecord : paymentRecords) {
			// paymentRecord의 날짜(년월일)를 추출
			LocalDate paymentDate = paymentRecord.getPaymentDate().toLocalDate();

			// Map에서 해당 날짜에 맞는 TripDayId 찾기
			Long tripDayId = tripDayMap.get(paymentDate);

			if (tripDayId != null) {
				// 일치하는 TripDayId가 있을 경우 업데이트
				paymentRecordMapper.updateTripDayId(paymentRecord.getRecordId(), tripDayId);
				updatedCount++;
			} else {
				log.warn("PaymentRecord(id: {})의 날짜({})에 해당하는 TripDay가 존재하지 않습니다.",
						paymentRecord.getRecordId(), paymentDate);
			}
		}
		// 5. 결과 반환
		log.info("tripId({})에 대한 총 {}건의 결제 기록이 TripDay와 연동되었습니다.", tripId, updatedCount);
		return updatedCount > 0;
	}
}
