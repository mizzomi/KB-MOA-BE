package org.moa.reservation.transport.exception;

import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;

/**
 * 교통 예약 관련 비즈니스 예외
 * 기존 IllegalArgumentException, IllegalStateException을 대체
 */
public class TransportReservationException extends BusinessException {
    
    public TransportReservationException(StatusCode statusCode, String message) {
        super(statusCode, message);
    }
    
    // 좌석 관련 예외
    public static TransportReservationException seatNotAvailable() {
        return new TransportReservationException(
            StatusCode.CONFLICT, 
            "선택한 좌석 중 이미 예약 중인 좌석이 있습니다."
        );
    }
    
    public static TransportReservationException seatNotFound() {
        return new TransportReservationException(
            StatusCode.NOT_FOUND,
            "선택한 좌석 중 존재하지 않는 좌석이 있습니다. 다시 확인해 주세요."
        );
    }
    
    // Trip 관련 예외
    public static TransportReservationException tripDayNotFound() {
        return new TransportReservationException(
            StatusCode.NOT_FOUND,
            "해당 날짜의 trip_day가 존재하지 않습니다."
        );
    }
    
    // 예약 관련 예외
    public static TransportReservationException reservationNotFound(Long reservationId) {
        return new TransportReservationException(
            StatusCode.NOT_FOUND,
            "해당 reservationId [" + reservationId + "] 에 해당하는 예약이 존재하지 않습니다."
        );
    }
    
    public static TransportReservationException unauthorizedAccess() {
        return new TransportReservationException(
            StatusCode.FORBIDDEN,
            "해당 사용자의 예약건이 아닙니다."
        );
    }
    
    public static TransportReservationException unauthorizedCancel() {
        return new TransportReservationException(
            StatusCode.FORBIDDEN,
            "본인의 예약만 취소할 수 있습니다."
        );
    }
    
    // 결제 관련 예외
    public static TransportReservationException paymentAmountMismatch() {
        return new TransportReservationException(
            StatusCode.BAD_REQUEST,
            "입력된 금액이 실제 결제될 금액과 다릅니다."
        );
    }
    
    public static TransportReservationException paymentFailed() {
        return new TransportReservationException(
            StatusCode.INTERNAL_ERROR,
            "예약확정으로 변경된 좌석이 없습니다. 다시 시도해주세요."
        );
    }
    
    // 취소 관련 예외
    public static TransportReservationException noCancellableSeats() {
        return new TransportReservationException(
            StatusCode.CONFLICT,
            "취소 가능한 좌석이 없습니다. 이미 결제되었거나 취소된 상태입니다."
        );
    }
}
