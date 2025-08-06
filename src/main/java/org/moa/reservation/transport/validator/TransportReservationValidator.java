package org.moa.reservation.transport.validator;

import org.moa.reservation.transport.dto.TransResStatusDto;
import org.moa.reservation.transport.exception.TransportReservationException;
import org.moa.reservation.transport.mapper.TransportMapper;
import org.moa.reservation.transport.type.Status;
import org.moa.reservation.mapper.ReservationMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 교통 예약 관련 검증 로직을 담당하는 Validator
 * ServiceImpl에서 분리한 검증 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransportReservationValidator {
    
    private final TransportMapper transportMapper;
    private final ReservationMapper reservationMapper;
    
    /**
     * 좌석 예약 가능 여부 검증
     * @param tranResIds 예약할 좌석 ID 목록
     */
    public void validateSeatsForReservation(List<Long> tranResIds) {
        log.info("업데이트할 tran_res_id 목록: {}", tranResIds);
        
        // 1. 좌석 상태 확인
        List<TransResStatusDto> seatStatuses = transportMapper.findStatusesByIds(tranResIds);
        
        boolean allAvailable = seatStatuses.stream()
            .allMatch(seat -> seat.getStatus() == Status.AVAILABLE);
        
        if (!allAvailable) {
            throw TransportReservationException.seatNotAvailable();
        }
        
        // 2. 좌석 존재 여부 확인
        List<Long> existingIds = transportMapper.findExistingTranResIds(tranResIds);
        
        if (existingIds.size() != tranResIds.size()) {
            List<Long> missingIds = new ArrayList<>(tranResIds);
            missingIds.removeAll(existingIds);
            log.warn("존재하지 않는 좌석 ID: {}", missingIds);
            throw TransportReservationException.seatNotFound();
        }
    }
    
    /**
     * 예약 소유권 검증 (결제용)
     * @param memberId 현재 사용자 ID
     * @param reservationId 예약 ID
     * @return 예약 소유자 ID
     */
    public Long validateReservationOwnershipForPayment(Long memberId, Long reservationId) {
        Long ownerMemberId = reservationMapper.findMemberIdByReservationId(reservationId);
        
        if (ownerMemberId == null) {
            throw TransportReservationException.reservationNotFound(reservationId);
        }
        
        if (!ownerMemberId.equals(memberId)) {
            throw TransportReservationException.unauthorizedAccess();
        }
        
        return ownerMemberId;
    }
    
    /**
     * 예약 소유권 검증 (취소용)
     * @param memberId 현재 사용자 ID
     * @param reservationId 예약 ID
     */
    public void validateReservationOwnershipForCancel(Long memberId, Long reservationId) {
        Long ownerMemberId = reservationMapper.findMemberIdByReservationId(reservationId);
        
        if (ownerMemberId == null || !ownerMemberId.equals(memberId)) {
            throw TransportReservationException.unauthorizedCancel();
        }
    }
    
    /**
     * 결제 금액 일치 여부 검증
     * @param expectedAmount 예상 금액 (DB에서 계산된 금액)
     * @param actualAmount 실제 요청 금액
     */
    public void validatePaymentAmount(BigDecimal expectedAmount, BigDecimal actualAmount) {
        if (expectedAmount.compareTo(actualAmount) != 0) {
            log.warn("결제 금액 불일치 - 예상: {}, 실제: {}", expectedAmount, actualAmount);
            throw TransportReservationException.paymentAmountMismatch();
        }
    }
}
