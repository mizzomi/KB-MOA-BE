package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {
    @Mock
    private SettlementMapper settlementMapper;
    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private AccountMapper accountMapper;
    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Nested
    @DisplayName("createSettlement() 메서드")
    class CreateSettlementTest{
        private Long expenseId;
        private Long tripId;
        private Long creatorId;
        private Long memberId;
        private BigDecimal amount;
        @BeforeEach
        void setUp() {
            expenseId = 1L;
            tripId = 1L;
            creatorId = 1L;
            amount = new BigDecimal("15000.00");
            doNothing().when(settlementMapper).insert(any(SettlementNotes.class));
        }

        @Test
        @DisplayName("createSettlement() 메서드, creatorId != memberId")
        void createSettlement() {
            memberId = 2L;
            // when
            boolean result = settlementService.createSettlement(expenseId, tripId, creatorId, memberId, amount);

            // then
            assertThat(result).isTrue();

            // 1. settlementMapper.insert가 한 번 호출되었는지 검증 및 전달된 SettlementNotes 객체 확인
            ArgumentCaptor<SettlementNotes> noteCaptor = ArgumentCaptor.forClass(SettlementNotes.class);
            verify(settlementMapper, times(1)).insert(noteCaptor.capture());
            SettlementNotes capturedNote = noteCaptor.getValue();

            log.info(expenseId + ":" + tripId + ":" + memberId + ":" + amount);
            assertThat(capturedNote.getExpenseId()).isEqualTo(expenseId);
            assertThat(capturedNote.getTripId()).isEqualTo(tripId);
            assertThat(capturedNote.getMemberId()).isEqualTo(memberId);
            assertThat(capturedNote.getShareAmount()).isEqualTo(amount);
            assertThat(capturedNote.getIsPayed()).isFalse(); // 초기값 false 확인
            assertThat(capturedNote.getReceived()).isTrue(); // 받은내역 확인
        }

        @Test
        @DisplayName("createSettlement() 메서드, creatorId == memberId")
        void createSettlement2() {
            memberId = 1L;
            // when
            boolean result = settlementService.createSettlement(expenseId, tripId, creatorId, memberId, amount);

            // then
            assertThat(result).isTrue();

            // 1. settlementMapper.insert가 한 번 호출되었는지 검증 및 전달된 SettlementNotes 객체 확인
            ArgumentCaptor<SettlementNotes> noteCaptor = ArgumentCaptor.forClass(SettlementNotes.class);
            verify(settlementMapper, times(1)).insert(noteCaptor.capture());
            SettlementNotes capturedNote = noteCaptor.getValue();

            log.info(expenseId + ":" + tripId + ":" + memberId + ":" + amount);
            assertThat(capturedNote.getExpenseId()).isEqualTo(expenseId);
            assertThat(capturedNote.getTripId()).isEqualTo(tripId);
            assertThat(capturedNote.getMemberId()).isEqualTo(memberId);
            assertThat(capturedNote.getShareAmount()).isEqualTo(amount);
            assertThat(capturedNote.getIsPayed()).isTrue(); // 초기값 false 확인
            assertThat(capturedNote.getReceived()).isFalse(); // 보낸내역 확인
        }
    }

    @Nested
    @DisplayName("getSettlementProgress() 메서드")
    class GetSettlementProgressTest{
        // getSettlementProgress 메서드 테스트에 필요한 필드들
        private Long mockExpenseId; // 각 테스트마다 별도의 expenseId 사용

        @BeforeEach // 이 @Nested 클래스의 각 테스트 메서드 실행 전 초기화
        void setUpGetSettlementProgress() {
            mockExpenseId = 1L; // 가상의 expenseId 설정
            // 이 setUp에서는 mapper mocking을 하지 않고, 각 테스트 메서드 내에서 필요한 Mock 데이터와 스터빙을 설정합니다.
        }

        @Test
        @DisplayName("정산 진행 상황 조회 성공 시, 올바른 DTO가 반환되어야 한다")
        void getSettlementProgress_success_returnsCorrectDto() {
            // given (테스트에 필요한 Mock 데이터 준비)

            // 1. expenseMapper.searchByExpenseId 호출 시 반환될 가상의 Expense 객체
            Expense mockExpense = Expense.builder()
                    .expenseId(mockExpenseId)
                    .expenseName("저녁 식사")
                    .expenseDate(LocalDateTime.of(2025, 7, 24, 19, 0))
                    .amount(new BigDecimal("60000.00"))
                    .settlementCompleted(false) // 아직 정산 진행중이라고 가정
                    .build();
            when(expenseMapper.searchByExpenseId(eq(mockExpenseId))).thenReturn(mockExpense);

            // 2. settlementMapper.searchByExpenseId 호출 시 반환될 가상의 SettlementNotes 리스트
            List<SettlementNotes> mockSettlementNotesList = Arrays.asList(
                    // 멤버 1: 보낸 요청 (내가 비용을 냈고, 다른 애들이 나한테 줘야 할 돈 -> received=false)
                    SettlementNotes.builder().settlementId(101L).expenseId(mockExpenseId).memberId(100L)
                            .shareAmount(new BigDecimal("20000.00")).isPayed(true).received(false).build(),
                    // 멤버 2: 받은 요청, 지불 완료 (내가 갚을 돈인데 이미 냄)
                    SettlementNotes.builder().settlementId(102L).expenseId(mockExpenseId).memberId(200L)
                            .shareAmount(new BigDecimal("20000.00")).isPayed(true).received(true).build(),
                    // 멤버 3: 받은 요청, 지불 미완료 (내가 갚을 돈인데 아직 안 냄)
                    SettlementNotes.builder().settlementId(103L).expenseId(mockExpenseId).memberId(300L)
                            .shareAmount(new BigDecimal("20000.00")).isPayed(false).received(true).build()
            );
            when(settlementMapper.searchByExpenseId(eq(mockExpenseId))).thenReturn(mockSettlementNotesList);

            // 3. MemberMapper.getByMemberId 호출 시 반환될 가상의 Member 객체들
            when(memberMapper.getByMemberId(eq(100L))).thenReturn(Member.builder().memberId(100L).name("김철수").build());
            when(memberMapper.getByMemberId(eq(200L))).thenReturn(Member.builder().memberId(200L).name("이영희").build());
            when(memberMapper.getByMemberId(eq(300L))).thenReturn(Member.builder().memberId(300L).name("박민수").build());

            // when (테스트 대상 메서드 실행)
            SettlementProgressResponseDto result = settlementService.getSettlementProgress(mockExpenseId);

            // then (결과 검증)
            assertThat(result).isNotNull();
            assertThat(result.getExpenseName()).isEqualTo(mockExpense.getExpenseName());
            assertThat(result.getExpenseDate()).isEqualTo(mockExpense.getExpenseDate());
            assertThat(result.getAmount()).isEqualTo(mockExpense.getAmount());

            // names와 statuses 리스트 검증 (순서가 중요한 경우)
            // SettlementNotes가 조회되는 순서에 따라 names와 statuses의 순서가 결정됩니다.
            // Mock 데이터의 순서에 맞춰 검증합니다.
            assertThat(result.getNames()).containsExactly("김철수", "이영희", "박민수");
            // 상태 로직에 따라 예상되는 상태 텍스트 검증
            // 100L(김철수): received=false, isPayed=true(관계없음), expense.settlementCompleted=false -> "정산 진행중"
            // 200L(이영희): received=true, isPayed=true, expense.settlementCompleted=false -> "정산 진행중"
            // 300L(박민수): received=true, isPayed=false, expense.settlementCompleted=false -> "정산 하기"
            assertThat(result.getStatuses()).containsExactly("정산 진행중", "정산 진행중", "정산 하기");

            // 매퍼 호출 횟수 검증
            verify(expenseMapper, times(1)).searchByExpenseId(eq(mockExpenseId));
            verify(settlementMapper, times(1)).searchByExpenseId(eq(mockExpenseId));
            verify(memberMapper, times(3)).getByMemberId(any(Long.class)); // 3명의 멤버 이름 조회
        }
    }

    @Nested
    @DisplayName("settle() 메서드")
    class settleTest{
        private SettlementRequestDto validDto;
        private Expense mockExpense;
        private Account mockReceiverAccount;
        private Account mockSenderAccount;

        @BeforeEach // 이 @Nested 클래스의 각 테스트 메서드 실행 전 초기화
        void setUpSettle() {
            // 1. 유효한 SettlementRequestDto 설정
            validDto = SettlementRequestDto.builder()
                    .expenseId(1L)
                    .memberId(200L) // 송신자 (돈을 보내는 사람)
                    .amount(new BigDecimal("10000.00")) // 송금할 금액
                    .build();

            // 2. expenseMapper.searchByExpenseId 호출 시 반환될 가상의 Expense 객체
            //    (Expense의 memberId는 수신자 ID가 됨)
            mockExpense = Expense.builder()
                    .expenseId(1L)
                    .memberId(100L) // 수신자 (돈을 받을 사람)
                    .expenseName("저녁 식사")
                    .amount(new BigDecimal("30000.00"))
                    .build();
            when(expenseMapper.searchByExpenseId(eq(validDto.getExpenseId()))).thenReturn(mockExpense);

            // 3. accountMapper.searchAccountByMemberId 호출 시 반환될 가상의 Account 객체들
            //    (잔액이 충분하도록 설정)
            mockReceiverAccount = Account.builder()
                    .memberId(100L).balance(new BigDecimal("50000.00")).build(); // 수신자 계좌
            mockSenderAccount = Account.builder()
                    .memberId(200L).balance(new BigDecimal("15000.00")).build(); // 송신자 계좌 (잔액 충분)
        }

        @Test
        @DisplayName("정산 처리 성공 시, 계좌 트랜잭션이 올바르게 호출되어야 한다")
        void settle() {
            // given
            // accountMapper.searchAccountByMemberId 스터빙 (성공 케이스에 필요)
            when(accountMapper.searchAccountByMemberId(eq(mockExpense.getMemberId()))).thenReturn(mockReceiverAccount); // 수신자 계좌
            when(accountMapper.searchAccountByMemberId(eq(validDto.getMemberId()))).thenReturn(mockSenderAccount);     // 송신자 계좌

            // accountMapper.transactionBalance 스터빙 (성공 케이스에서만 호출)
            doNothing().when(accountMapper).transactionBalance(any(Long.class), any(Long.class), any(BigDecimal.class));


            // when (테스트 대상 메서드 실행)
            boolean result = settlementService.settle(validDto);

            // then (결과 검증)
            assertThat(result).isTrue(); // 메서드가 true를 반환하는지 확인

            // 1. expenseMapper.searchByExpenseId가 한 번 호출되었는지 검증
            verify(expenseMapper, times(1)).searchByExpenseId(eq(validDto.getExpenseId()));

            // 2. accountMapper.searchAccountByMemberId가 수신자와 송신자 각각에 대해 한 번씩 호출되었는지 검증
            verify(accountMapper, times(1)).searchAccountByMemberId(eq(mockExpense.getMemberId())); // 수신자 ID로 호출
            verify(accountMapper, times(1)).searchAccountByMemberId(eq(validDto.getMemberId()));     // 송신자 ID로 호출

            // 3. accountMapper.transactionBalance가 올바른 인자들과 함께 한 번 호출되었는지 검증
            verify(accountMapper, times(1)).transactionBalance(
                    eq(mockExpense.getMemberId()), // 수신자 ID
                    eq(validDto.getMemberId()),    // 송신자 ID
                    eq(validDto.getAmount())       // 금액
            );

            // (선택 사항) 다른 매퍼나 서비스가 호출되지 않았음을 검증
            verifyNoMoreInteractions(expenseMapper); // expenseMapper에 다른 호출이 없음을 확인
            verifyNoMoreInteractions(accountMapper); // accountMapper에 다른 호출이 없음을 확인
            verifyNoInteractions(settlementMapper); // settle 메서드에서 settlementMapper는 사용하지 않으므로 호출 없어야 함
            verifyNoInteractions(memberMapper);     // settle 메서드에서 memberMapper는 사용하지 않으므로 호출 없어야 함
        }

        @Test
        @DisplayName("잔액 부족시, 예외가 처리된다.")
        void settleFailure() {
            // given
            // 송신자 계좌 (잔액 부족)를 이 테스트 내에서만 설정
            mockSenderAccount = Account.builder()
                    .memberId(200L).balance(new BigDecimal("5000.00")).build();
            when(accountMapper.searchAccountByMemberId(eq(mockExpense.getMemberId()))).thenReturn(mockReceiverAccount); // 수신자 계좌
            when(accountMapper.searchAccountByMemberId(eq(validDto.getMemberId()))).thenReturn(mockSenderAccount);     // 송신자 계좌 (잔액 부족)

            // when & then
            BusinessException e = assertThrows(BusinessException.class, () ->
                    settlementService.settle(validDto)
            );

            assertThat(e.getStatusCode()).isEqualTo(StatusCode.BAD_REQUEST);
            assertThat(e.getMessage()).isEqualTo("계좌 잔액을 확인해주세요.");

            // 매퍼 호출 검증 (여기까지는 성공했어야 함)
            verify(expenseMapper, times(1)).searchByExpenseId(eq(validDto.getExpenseId()));
            verify(accountMapper, times(1)).searchAccountByMemberId(eq(mockExpense.getMemberId()));
            verify(accountMapper, times(1)).searchAccountByMemberId(eq(validDto.getMemberId()));
            verifyNoMoreInteractions(expenseMapper);
            // transactionBalance는 호출되지 않아야 함
            verify(accountMapper, never()).transactionBalance(any(Long.class), any(Long.class), any(BigDecimal.class));
        }
    }
}