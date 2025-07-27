package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.trip.dto.expense.AmountAndMemberIdRequest;
import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.expense.ExpenseResponseDto;
import org.moa.trip.entity.Expense;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.entity.Trip;
import org.moa.trip.mapper.ExpenseMapper;
import org.moa.trip.mapper.SettlementMapper;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.type.Location;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {
    @Mock
    private SettlementMapper settlementMapper;
    @Mock
    private SettlementService settlementService;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private Long mockMemberId;
    private Long mockTripId;

    @Nested
    @DisplayName("createExpense() 메서드")
    class CreateExpenseTest {
        private ExpenseCreateRequestDto dto;
        private Trip mockTrip;

        @BeforeEach
        void setUpCreateExpense() {
            dto = ExpenseCreateRequestDto.builder()
                    .tripId(1L)
                    .memberId(1L)
                    .expenseName("점심 식사 값")
                    .amount(new BigDecimal("30000.00"))
                    .expenses(Arrays.asList(
                            new AmountAndMemberIdRequest(1L,new BigDecimal(10000.00)),
                            new AmountAndMemberIdRequest(1L,new BigDecimal(10000.00)),
                            new AmountAndMemberIdRequest(1L,new BigDecimal(10000.00))
                    ))
                    .build();

            mockTrip = Trip.builder()
                    .tripId(1L)
                    .tripName("부산 여행")
                    .tripLocation(Location.BUSAN)
                    .build();

            when(tripMapper.searchTripById(any(Long.class))).thenReturn(mockTrip);

            doAnswer(invocation -> {
                Expense capturedExpense = invocation.getArgument(0);
                try {
                    Field expenseIdField = Expense.class.getDeclaredField("expenseId");
                    expenseIdField.setAccessible(true);
                    expenseIdField.set(capturedExpense, 500L);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to set expenseId via reflection in test mock.", e);
                }
                return null;
            }).when(expenseMapper).insert(any(Expense.class));
        }
        @Test
        @DisplayName("비용 생성 성공시 Expense, Settlement 가 저장된다")
        void createExpense() {
            //given
            //when
            boolean result = expenseService.createExpense(dto);
            //then
            assertThat(result).isTrue();

            verify(tripMapper, times(1)).searchTripById(eq(dto.getTripId()));

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseMapper, times(1)).insert(expenseCaptor.capture());
            Expense capturedExpense = expenseCaptor.getValue();

            assertThat(capturedExpense.getTripId()).isEqualTo(dto.getTripId());
            assertThat(capturedExpense.getMemberId()).isEqualTo(dto.getMemberId());
            assertThat(capturedExpense.getExpenseName()).isEqualTo(dto.getExpenseName());
            assertThat(capturedExpense.getAmount()).isEqualTo(dto.getAmount());
            assertThat(capturedExpense.getLocation()).isEqualTo(mockTrip.getTripLocation());
            assertThat(capturedExpense.getSettlementCompleted()).isFalse();
            assertThat(capturedExpense.getExpenseId()).isNotNull(); // 가상의 ID가 설정되었는지 확인

            log.info("임시 생성 아이디:{}",capturedExpense.getExpenseId());

            verify(settlementService, times(dto.getExpenses().size()))
                    .createSettlement(
                            eq(capturedExpense.getExpenseId()), // 리플렉션으로 설정된 expenseId가 전달됨
                            eq(dto.getTripId()),
                            any(Long.class),
                            any(Long.class),
                            any(BigDecimal.class)
                    );
        }
    }

    @Nested
    @DisplayName("getExpenses() 메서드")
    class GetExpensesTest {
        @BeforeEach
        void setUp() {
            mockMemberId = 1L;
            mockTripId = 1L;
        }
        @Test
        @DisplayName("정산 내역 조회 성공 시, 올바른 DTO 리스트와 상태가 반환되어야 한다")
        void getExpenses_success_returnsCorrectDtoListAndStatuses() {
            // given (테스트에 필요한 Mock 데이터 준비)

            // 1. settlementMapper.searchByMemberIdAndTripId 호출 시 반환될 가상의 SettlementNotes 리스트
            List<SettlementNotes> mockSettlementNotesList = new ArrayList<>();
            // -- 케이스 1: 보낸 요청, 정산 완료 (예상 상태: "정산 완료")
            mockSettlementNotesList.add(SettlementNotes.builder()
                    .settlementId(101L).expenseId(1L).shareAmount(new BigDecimal("18000.00"))
                    .received(false).isPayed(false).build()); // received=false일 때 isPayed는 상태 로직에 직접 관여 안함
            // -- 케이스 2: 보낸 요청, 정산 진행중 (예상 상태: "정산 진행중")
            mockSettlementNotesList.add(SettlementNotes.builder()
                    .settlementId(102L).expenseId(2L).shareAmount(new BigDecimal("5000.00"))
                    .received(false).isPayed(false).build());
            // -- 케이스 3: 받은 요청, 내가 냈고 전체 완료 (예상 상태: "정산 완료")
            mockSettlementNotesList.add(SettlementNotes.builder()
                    .settlementId(103L).expenseId(3L).shareAmount(new BigDecimal("10000.00"))
                    .received(true).isPayed(true).build());
            // -- 케이스 4: 받은 요청, 내가 냈지만 전체 미완료 (예상 상태: "정산 진행중")
            mockSettlementNotesList.add(SettlementNotes.builder()
                    .settlementId(104L).expenseId(4L).shareAmount(new BigDecimal("2500.00"))
                    .received(true).isPayed(true).build());
            // -- 케이스 5: 받은 요청, 내가 아직 안 갚음 (예상 상태: "정산 하기")
            mockSettlementNotesList.add(SettlementNotes.builder()
                    .settlementId(105L).expenseId(5L).shareAmount(new BigDecimal("7000.00"))
                    .received(true).isPayed(false).build());

            // settlementMapper.searchByMemberIdAndTripId 호출 시 위 리스트를 반환하도록 설정
            when(settlementMapper.searchByMemberIdAndTripId(eq(mockMemberId), eq(mockTripId)))
                    .thenReturn(mockSettlementNotesList);

            // 2. expenseMapper.searchByExpenseId 호출 시 반환될 가상의 Expense 객체들 설정
            // 각 expenseId에 맞는 settlementCompleted 상태를 가진 Expense 객체를 반환하도록 설정
            when(expenseMapper.searchByExpenseId(eq(1L))).thenReturn(Expense.builder()
                    .expenseId(1L).expenseDate(LocalDateTime.of(2025, 3, 4, 8, 30))
                    .amount(new BigDecimal("54000.00"))
                    .settlementCompleted(true).build()); // 케이스 1의 expected: true
            when(expenseMapper.searchByExpenseId(eq(2L))).thenReturn(Expense.builder()
                    .expenseId(2L).expenseDate(LocalDateTime.of(2025, 3, 5, 10, 0))
                    .amount(new BigDecimal("20000.00"))
                    .settlementCompleted(false).build()); // 케이스 2의 expected: false
            when(expenseMapper.searchByExpenseId(eq(3L))).thenReturn(Expense.builder()
                    .expenseId(3L).expenseDate(LocalDateTime.of(2025, 3, 6, 12, 0))
                    .settlementCompleted(true).build()); // 케이스 3의 expected: true
            when(expenseMapper.searchByExpenseId(eq(4L))).thenReturn(Expense.builder()
                    .expenseId(4L).expenseDate(LocalDateTime.of(2025, 3, 7, 14, 0))
                    .settlementCompleted(false).build()); // 케이스 4의 expected: false
            when(expenseMapper.searchByExpenseId(eq(5L))).thenReturn(Expense.builder()
                    .expenseId(5L).expenseDate(LocalDateTime.of(2025, 3, 8, 16, 0))
                    .settlementCompleted(false).build()); // 케이스 5의 expected: false


            // when (테스트 대상 메서드 실행)
            List<ExpenseResponseDto> result = expenseService.getExpenses(mockMemberId, mockTripId);

            // then (결과 검증)
            assertThat(result).isNotNull();
            assertThat(result).hasSize(5); // 5개의 정산 내역이 DTO로 변환되어 반환되어야 함

            // 각 DTO의 상태 텍스트가 예상과 일치하는지 검증
            assertThat(result.get(0).getStatus()).isEqualTo("정산 완료");    // 케이스 1: 보낸 요청(false), 전체 완료(true) -> 정산 완료
            assertThat(result.get(1).getStatus()).isEqualTo("정산 진행중");   // 케이스 2: 보낸 요청(false), 전체 미완료(false) -> 정산 진행중
            assertThat(result.get(2).getStatus()).isEqualTo("정산 완료");    // 케이스 3: 받은 요청(true), 내가 냈고(true), 전체 완료(true) -> 정산 완료
            assertThat(result.get(3).getStatus()).isEqualTo("정산 진행중");   // 케이스 4: 받은 요청(true), 내가 냈고(true), 전체 미완료(false) -> 정산 진행중
            assertThat(result.get(4).getStatus()).isEqualTo("정산 하기");     // 케이스 5: 받은 요청(true), 내가 안 냈음(false), 전체 미완료(false) -> 정산 하기

            // 매퍼 호출 횟수 검증
            verify(settlementMapper, times(1)).searchByMemberIdAndTripId(eq(mockMemberId), eq(mockTripId));
            // expenseMapper.searchByExpenseId는 SettlementNotes 리스트의 각 항목에 대해 호출되므로 5번 호출
            verify(expenseMapper, times(mockSettlementNotesList.size())).searchByExpenseId(any(Long.class));
        }
    }
}