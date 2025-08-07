package org.moa.reservation.transport.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moa.reservation.mapper.ReservationMapper;
import org.moa.reservation.transport.dto.TransResStatusDto;
import org.moa.reservation.transport.exception.TransportReservationException;
import org.moa.reservation.transport.mapper.TransportMapper;
import org.moa.reservation.transport.type.Status;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportReservationValidatorTest {

    @Mock
    private TransportMapper transportMapper;
    
    @Mock
    private ReservationMapper reservationMapper;
    
    @InjectMocks
    private TransportReservationValidator validator;
    
    @Test
    @DisplayName("모든 좌석이 예약 가능한 경우 검증 통과")
    void validateSeatsForReservation_AllAvailable_Success() {
        // Given
        List<Long> tranResIds = Arrays.asList(1L, 2L, 3L);
        List<TransResStatusDto> availableSeats = Arrays.asList(
            createSeatStatus(1L, Status.AVAILABLE),
            createSeatStatus(2L, Status.AVAILABLE),
            createSeatStatus(3L, Status.AVAILABLE)
        );
        
        when(transportMapper.findStatusesByIds(tranResIds)).thenReturn(availableSeats);
        when(transportMapper.findExistingTranResIds(tranResIds)).thenReturn(tranResIds);
        
        // When & Then
        assertDoesNotThrow(() -> validator.validateSeatsForReservation(tranResIds));
    }
    
    @Test
    @DisplayName("일부 좌석이 이미 예약된 경우 예외 발생")
    void validateSeatsForReservation_SomeNotAvailable_ThrowsException() {
        // Given
        List<Long> tranResIds = Arrays.asList(1L, 2L, 3L);
        List<TransResStatusDto> mixedSeats = Arrays.asList(
            createSeatStatus(1L, Status.AVAILABLE),
            createSeatStatus(2L, Status.PENDING),  // 이미 예약 중
            createSeatStatus(3L, Status.AVAILABLE)
        );
        
        when(transportMapper.findStatusesByIds(tranResIds)).thenReturn(mixedSeats);
        
        // When & Then
        assertThrows(TransportReservationException.class, 
            () -> validator.validateSeatsForReservation(tranResIds));
    }
    
    @Test
    @DisplayName("존재하지 않는 좌석 ID가 포함된 경우 예외 발생")
    void validateSeatsForReservation_NonExistentSeats_ThrowsException() {
        // Given
        List<Long> tranResIds = Arrays.asList(1L, 2L, 999L);  // 999L은 존재하지 않음
        List<Long> existingIds = Arrays.asList(1L, 2L);
        List<TransResStatusDto> availableSeats = Arrays.asList(
            createSeatStatus(1L, Status.AVAILABLE),
            createSeatStatus(2L, Status.AVAILABLE)
        );
        
        when(transportMapper.findStatusesByIds(tranResIds)).thenReturn(availableSeats);
        when(transportMapper.findExistingTranResIds(tranResIds)).thenReturn(existingIds);
        
        // When & Then
        assertThrows(TransportReservationException.class, 
            () -> validator.validateSeatsForReservation(tranResIds));
    }
    
    @Test
    @DisplayName("결제 금액이 일치하는 경우 검증 통과")
    void validatePaymentAmount_Matching_Success() {
        // Given
        BigDecimal expectedAmount = new BigDecimal("50000");
        BigDecimal actualAmount = new BigDecimal("50000");
        
        // When & Then
        assertDoesNotThrow(() -> validator.validatePaymentAmount(expectedAmount, actualAmount));
    }
    
    @Test
    @DisplayName("결제 금액이 일치하지 않는 경우 예외 발생")
    void validatePaymentAmount_NotMatching_ThrowsException() {
        // Given
        BigDecimal expectedAmount = new BigDecimal("50000");
        BigDecimal actualAmount = new BigDecimal("45000");
        
        // When & Then
        assertThrows(TransportReservationException.class,
            () -> validator.validatePaymentAmount(expectedAmount, actualAmount));
    }
    
    @Test
    @DisplayName("예약 소유자가 맞는 경우 검증 통과")
    void validateReservationOwnershipForPayment_ValidOwner_Success() {
        // Given
        Long memberId = 1L;
        Long reservationId = 100L;
        
        when(reservationMapper.findMemberIdByReservationId(reservationId)).thenReturn(memberId);
        
        // When
        Long result = validator.validateReservationOwnershipForPayment(memberId, reservationId);
        
        // Then
        assertEquals(memberId, result);
    }
    
    @Test
    @DisplayName("예약이 존재하지 않는 경우 예외 발생")
    void validateReservationOwnershipForPayment_NotFound_ThrowsException() {
        // Given
        Long memberId = 1L;
        Long reservationId = 999L;
        
        when(reservationMapper.findMemberIdByReservationId(reservationId)).thenReturn(null);
        
        // When & Then
        assertThrows(TransportReservationException.class,
            () -> validator.validateReservationOwnershipForPayment(memberId, reservationId));
    }
    
    @Test
    @DisplayName("다른 사용자의 예약인 경우 예외 발생")
    void validateReservationOwnershipForPayment_WrongOwner_ThrowsException() {
        // Given
        Long memberId = 1L;
        Long reservationId = 100L;
        Long actualOwnerId = 2L;
        
        when(reservationMapper.findMemberIdByReservationId(reservationId)).thenReturn(actualOwnerId);
        
        // When & Then
        assertThrows(TransportReservationException.class,
            () -> validator.validateReservationOwnershipForPayment(memberId, reservationId));
    }
    
    // Helper method
    private TransResStatusDto createSeatStatus(Long id, Status status) {
        TransResStatusDto dto = new TransResStatusDto();
        dto.setTranResId(id);
        dto.setStatus(status);
        return dto;
    }
}
