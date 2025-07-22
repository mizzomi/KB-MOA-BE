package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.mapper.SettlementMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {
    @Mock
    private SettlementMapper settlementMapper;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    private Long expenseId;
    private Long tripId;
    private Long memberId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        expenseId = 1L;
        tripId = 1L;
        memberId = 1L;
        amount = new BigDecimal("15000.00");
        doNothing().when(settlementMapper).insert(any(SettlementNotes.class));
    }

    @Test
    @DisplayName("정산 내역 성공 시, SettlementNotes 가 DB에 저장 잘 되는지?")
    void createSettlement() {
        // when
        boolean result = settlementService.createSettlement(expenseId, tripId, memberId, amount);

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
    }
}