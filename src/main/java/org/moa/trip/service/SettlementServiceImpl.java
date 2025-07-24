package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.account.entity.Account;
import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.dto.settlement.SettlementProgressResponseDto;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.moa.trip.entity.Expense;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.mapper.ExpenseMapper;
import org.moa.trip.mapper.SettlementMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.moa.trip.service.ExpenseServiceImpl.getString;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements  SettlementService {
    private final SettlementMapper settlementMapper;
    private final ExpenseMapper expenseMapper;
    private final MemberMapper memberMapper;
    private final AccountMapper accountMapper;

    public boolean createSettlement(Long expenseId, Long tripId, Long creatorId, Long memberId, BigDecimal amount) {
        log.info("createSettlement");
        SettlementNotes settlementNotes = SettlementNotes.builder()
                .expenseId(expenseId)
                .tripId(tripId)
                .memberId(memberId)
                .shareAmount(amount)
                // 정산 생성자 ID = 리스트에 담긴 ID -> 자동 정산 완료 처리
                // 정산 생성자 ID != 리스트에 담긴 ID -> 정산 해야함
                .isPayed(creatorId.equals(memberId))
                // 정산 생성자 ID == 리스트에 담긴 ID -> 정산요청을 보낸것임
                // 정산 생성자 ID != 리스트에 담긴 ID -> 정산요청을 받은것임
                .received(!creatorId.equals(memberId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try{
            settlementMapper.insert(settlementNotes);
            // 이때 여기서 사용자에게 정산 요청 알림을 생성
        } catch  (Exception e) {
            log.error("정산 내역 저장 중 데이터베이스 오류 발생: {}", e.getMessage());
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 저장 중 서버에 문제가 발생했습니다.");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean settle(SettlementRequestDto dto){
        log.info("settle");
        Expense expense = expenseMapper.searchByExpenseId(dto.getExpenseId());
        Long receiverId = expense.getMemberId();
        Long senderId = dto.getMemberId();
        Account receiverAccount;
        Account senderAccount;
        try {
            receiverAccount = accountMapper.searchAccountByMemberId(receiverId);
            senderAccount = accountMapper.searchAccountByMemberId(senderId);
        } catch (DataAccessException e) {
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "연동 계좌 조회 중 서버 오류가 발생했습니다.");
        }
        // 간단한 계산 로직
        BigDecimal amount = verificationAmount(dto, senderAccount, receiverAccount);
        accountMapper.transactionBalance(receiverId,senderId,amount);
        return true;
    }

    private static BigDecimal verificationAmount(SettlementRequestDto dto, Account senderAccount, Account receiverAccount) {
        BigDecimal amount = dto.getAmount();
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new BusinessException(StatusCode.BAD_REQUEST, "금액은 0원 이상이여야 합니다.");
        }
        if(senderAccount == null || receiverAccount == null){
            throw new BusinessException(StatusCode.BAD_REQUEST, "연동된 계좌를 찾을 수 없습니다.");
        }
        if(senderAccount.getBalance().subtract(amount).compareTo(BigDecimal.ZERO)<=0){
            throw new BusinessException(StatusCode.BAD_REQUEST, "계좌 잔액을 확인해주세요.");
        }
        return amount;
    }

    @Override
    @Transactional
    public SettlementProgressResponseDto getSettlementProgress(Long expenseId) {
        Expense expense =  expenseMapper.searchByExpenseId(expenseId);
        log.info("getSettlementProgress 호출 : expenseId={}", expenseId);
        if (expenseId == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST,"여행 ID가 누락되었습니다");
        }

        // 해당 비용에 연결된 정산 내역들을 불러옴
        List<SettlementNotes> settlementNotes;
        try {
            settlementNotes = settlementMapper.searchByExpenseId(expenseId);
        } catch (DataAccessException e) {
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 내역 조회 중 서버 오류가 발생했습니다.");
        }

        String expenseName = expense.getExpenseName();
        LocalDateTime expenseDate = expense.getExpenseDate();
        BigDecimal amount = expense.getAmount();
        List<String> names =  new ArrayList<>();
        List<String> statuses =  new ArrayList<>();

        for(SettlementNotes s : settlementNotes){
            Member member = memberMapper.getByMemberId(s.getMemberId());
            names.add(member.getName());
            statuses.add(getString(s,expense,s.getReceived()));
        }

        return SettlementProgressResponseDto.builder()
                .expenseName(expenseName)
                .expenseDate(expenseDate)
                .amount(amount)
                .names(names)
                .statuses(statuses)
                .build();
    }
}
