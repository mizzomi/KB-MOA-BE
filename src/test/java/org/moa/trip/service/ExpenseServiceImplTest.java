package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.trip.dto.settlement.ExpenseCreateRequestDto;
import org.moa.trip.entity.Expense;
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
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {
    @Mock
    private SettlementService settlementService;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private ExpenseCreateRequestDto dto;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        dto = ExpenseCreateRequestDto.builder()
                .tripId(1L)
                .memberId(1L)
                .expenseName("점심 식사 값")
                .amount(new BigDecimal("30000.00"))
                .memberIds(Arrays.asList(100L,200L,300L))
                .amounts(Arrays.asList(new BigDecimal("7500.00"),new BigDecimal("7500.00"),new BigDecimal("7500.00")))
                .build();

        mockTrip = Trip.builder()
                .tripId(1L)
                .tripName("부산 여행")
                .tripLocation(Location.BUSAN)
                .build();

        when(tripMapper.selectTripById(any(Long.class))).thenReturn(mockTrip);

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

        verify(tripMapper, times(1)).selectTripById(eq(dto.getTripId()));

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

        verify(settlementService, times(dto.getMemberIds().size()))
                .createSettlement(
                        eq(capturedExpense.getExpenseId()), // 리플렉션으로 설정된 expenseId가 전달됨
                        eq(dto.getTripId()),
                        any(Long.class),
                        any(BigDecimal.class)
                );
    }
}