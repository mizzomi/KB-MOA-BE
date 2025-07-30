package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.account.entity.Account;
import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.dto.settlement.ProgressAndMemberNameResponse;
import org.moa.trip.dto.settlement.SettlementInfoResponseDto;
import org.moa.trip.dto.settlement.SettlementProgressResponseDto;
import org.moa.trip.dto.settlement.SettlementRequestDto;
import org.moa.trip.entity.Expense;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.mapper.ExpenseMapper;
import org.moa.trip.mapper.SettlementMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.moa.trip.service.ExpenseServiceImpl.getString;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements  SettlementService {
    private final SettlementMapper settlementMapper;
    private final ExpenseMapper expenseMapper;
    private final MemberMapper memberMapper;
    private final AccountMapper accountMapper;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

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
    public SettlementInfoResponseDto getSettlementInfo(Long expenseId){
        // 1. 결제에 있는 결제자 ID = 보내야 하는사람 ID
        log.info("expenseId : {}",expenseId);
        Expense expense = expenseMapper.searchByExpenseId(expenseId);
        Member receiver = memberMapper.getByMemberId(expense.getMemberId());
        log.info("receiverId : {}",expense.getMemberId());
        // 2. 내 계좌번호 찾기(SecurityContext 에서 가져오기로 수정해야함)
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member sender = memberMapper.getByEmail(userDetails.getUsername());
        log.info("senderId : {}",sender.getMemberId());
        Account myaccount = accountMapper.searchAccountByMemberId(sender.getMemberId());
        log.info("account's owner Id : {}",myaccount.getMemberId());
        SettlementNotes settlementNotes = settlementMapper.searchByMemberIdAndExpenseId(expenseId,sender.getMemberId());
        return SettlementInfoResponseDto.builder()
                .receiverName(receiver.getName())
                .balance(myaccount.getBalance())
                .shareAmount(settlementNotes.getShareAmount())
                .build();
    }

    @Override
    @Transactional
    public boolean settle(SettlementRequestDto dto){
        log.info("settle 메서드 호출 시작: {}", dto);

        // 1. 요청 DTO 유효성 검사 (금액)
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("settle 실패: 금액이 유효하지 않습니다. amount={}", dto.getAmount());
            throw new BusinessException(StatusCode.BAD_REQUEST, "금액은 0원 이상이여야 합니다.");
        }

        for(int retryCount = 0; retryCount < MAX_RETRIES ; retryCount++){
            try{
                Expense expense = expenseMapper.searchByExpenseId(dto.getExpenseId());
                if (expense == null) {
                    log.warn("settle 실패: expenseId {}에 해당하는 Expense를 찾을 수 없습니다.", dto.getExpenseId());
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 처리 중 내부 데이터 오류가 발생했습니다. (관련 비용을 찾을 수 없음)");
                }

                Long receiverId = expense.getMemberId();
                Long senderId = dto.getMemberId();

                Account receiverAccount;
                Account senderAccount;

                // 2. 계좌 정보 조회 및 비관적 잠금 (Pessimistic Locking)
                //    데드락 방지를 위해 memberId 기준으로 일관된 순서로 잠금을 획득합니다.
                try {
                    if (senderId.compareTo(receiverId) < 0) { // senderId가 receiverId보다 작으면 sender 먼저 잠금
                        senderAccount = accountMapper.searchAccountByMemberIdForUpdate(senderId);
                        receiverAccount = accountMapper.searchAccountByMemberIdForUpdate(receiverId);
                    } else { // receiverId가 senderId보다 작으면 receiver 먼저 잠금 (ID가 같으면 순서 상관 없음)
                        receiverAccount = accountMapper.searchAccountByMemberIdForUpdate(receiverId);
                        senderAccount = accountMapper.searchAccountByMemberIdForUpdate(senderId);
                    }
                } catch (PessimisticLockingFailureException e) { // 잠금 획득 실패 (다른 트랜잭션이 이미 잠금)
                    log.warn("settle: 잠금 획득 실패 (비관적 잠금 경합). 재시도 #{}", retryCount + 1);
                    throw e; // 재시도 루프에 의해 다시 잡힘
                } catch (DataAccessException e) { // 기타 DB 오류
                    log.error("settle 실패: 계좌 조회 및 잠금 중 DB 오류 발생 - {}", e.getMessage(), e);
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "연동 계좌 조회 중 서버 오류가 발생했습니다.");
                }

                // 3. 계좌 존재 여부 및 잔액 검증
                if (senderAccount == null || receiverAccount == null) {
                    log.warn("settle 실패: 연동된 계좌를 찾을 수 없습니다. senderId={}, receiverId={}", senderId, receiverId);
                    throw new BusinessException(StatusCode.BAD_REQUEST, "연동된 계좌를 찾을 수 없습니다.");
                }
                // TODO : 계좌가 금액을 받을 수 있는지 여부 확인 -> 고도화 (예: 수신 계좌 활성화 여부 등)

                // 간단한 계산 로직
                BigDecimal amount = verificationAmount(dto, senderAccount, receiverAccount);
                log.info("amount : {}",amount);

                if (senderAccount.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                    log.warn("settle 실패: 계좌 잔액 부족. senderId={}, balance={}, amount={}", senderId, senderAccount.getBalance(), amount);
                    throw new BusinessException(StatusCode.BAD_REQUEST, "계좌 잔액을 확인해주세요.");
                }

                // 4. 계좌 간 잔액 이동 (이미 위에서 잠금 획득 완료)
                try {
                    accountMapper.transactionBalance(receiverId, senderId, amount);
                    log.info("settle: 계좌 트랜잭션 완료. senderId {} -> receiverId {} 에게 {}원 송금.", senderId, receiverId, amount);
                } catch (DataAccessException e) {
                    log.error("settle 실패: 계좌 이체 중 DB 오류 발생 - {}", e.getMessage(), e);
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "계좌 이체 중 서버 오류가 발생했습니다.");
                }

                // 5. 송신자(sender)의 정산 내역 완료 처리 (isPayed -> true)
                try {
                    settlementMapper.updateIsPayedByExpenseIdAndMemberID(dto.getExpenseId(), senderId);
                    log.info("settle: SettlementNotes.isPayed 업데이트 완료. expenseId={}, memberId={}", dto.getExpenseId(), senderId);
                } catch (DataAccessException e) {
                    log.error("settle 실패: SETTLEMENT_NOTES is_payed 업데이트 중 DB 오류 발생 - {}", e.getMessage(), e);
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 내역 업데이트 중 서버 오류가 발생했습니다.");
                }

                // 6. Expense 에 대한 완료 처리 검사(isSettleCompleted -> true)
                try{
                    List<SettlementNotes> settlementNotes = settlementMapper.searchByExpenseId(dto.getExpenseId());
                    boolean allSettlementsPayed = true;
                    for(SettlementNotes settlementNote : settlementNotes) {
                        if(settlementNote.getIsPayed() == null ||  !settlementNote.getIsPayed()) {
                            allSettlementsPayed = false;
                            break;
                        }
                    }
                    if (allSettlementsPayed) {
                        // 모든 정산 내역이 지불 완료되었으면, Expense의 settlement_completed를 true로 업데이트
                        expenseMapper.updateSettlementCompleted(dto.getExpenseId(), true);
                        log.info("settle: Expense.settlement_completed 업데이트 완료. expenseId={}", dto.getExpenseId());
                        // TODO: 이 시점에 정산 완료 알림을 정산 그룹 멤버들에게 보낼 수 있습니다.
                        // notificationService.sendSettlementGroupCompleted(dto.getExpenseId());
                    }
                } catch (DataAccessException e) {
                    log.error("settle 실패: 전체 정산 완료 상태 업데이트 중 DB 오류 발생 - {}", e.getMessage(), e);
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "전체 정산 상태 업데이트 중 서버 오류가 발생했습니다.");
                }

                log.info("settle 메서드 완료: expenseId={}", dto.getExpenseId());
                return true; // 성공적으로 모든 단계를 완료하면 루프 종료
            } catch (BusinessException e) {
                // 비즈니스 로직 상의 예외는 재시도하지 않고 즉시 던집니다. (예: BAD_REQUEST, NOT_FOUND)
                throw e;
            } catch (PessimisticLockingFailureException e) {
                // 비관적 잠금 실패 시 재시도. (데드락, 락 타임아웃 등)
                log.warn("settle: 잠금 획득 실패 또는 데드락 발생. 재시도 시도 중 ({} / {}).", retryCount + 1, MAX_RETRIES);
                if (retryCount < MAX_RETRIES - 1) { // 마지막 시도가 아니면 대기 후 재시도
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * (retryCount + 1)); // 지수적 백오프 (간단화)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                        throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 처리 중 재시도 대기 오류가 발생했습니다.");
                    }
                } else { // 최대 재시도 횟수 초과
                    log.error("settle 실패: 최대 재시도 횟수 초과. expenseId={}", dto.getExpenseId(), e);
                    throw new BusinessException(StatusCode.INTERNAL_ERROR, "일시적인 서버 부하로 정산에 실패했습니다. 잠시 후 다시 시도해주세요.");
                }
            } catch (DataAccessException e) {
                // 기타 DataAccessException (PessimisticLockingFailureException 제외)도 내부 오류로 처리.
                // 여기서는 재시도하지 않는 것이 일반적입니다. (DB 연결 자체 문제 등은 재시도해도 안 될 가능성 높음)
                log.error("settle 실패: 알 수 없는 DB 오류 발생 (재시도 대상 아님) - {}", e.getMessage(), e);
                throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 처리 중 서버 오류가 발생했습니다.");
            }
        }
        // 이 코드에 도달하면 MAX_RETRIES가 0이거나, 루프가 이상하게 동작한 경우 (도달할 일 없음)
        return false; // 방어적 코드
    }

    private static BigDecimal verificationAmount(SettlementRequestDto dto, Account senderAccount, Account receiverAccount) {
        BigDecimal amount = dto.getAmount();
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new BusinessException(StatusCode.BAD_REQUEST, "금액은 0원 이상이여야 합니다.");
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
        List<ProgressAndMemberNameResponse> progresses =  new ArrayList<>();
        for(SettlementNotes s : settlementNotes){
            Member member = memberMapper.getByMemberId(s.getMemberId());
            progresses.add(ProgressAndMemberNameResponse.builder()
                    .name(member.getName())
                    .status(s.getIsPayed()?"정산 완료":"정산 진행중")
                    .build());
        }

        return SettlementProgressResponseDto.builder()
                .expenseName(expenseName)
                .expenseDate(expenseDate)
                .amount(amount)
                .progresses(progresses)
                .build();
    }
}
