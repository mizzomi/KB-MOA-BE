package org.moa.reservation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.dto.QrRestaurantReservationDto;
import org.moa.reservation.entity.Reservation;

@Mapper
public interface ReservationMapper {
	void insertReservation(Reservation reservation);

	Long findMemberIdByReservationId(@Param("reservationId") Long reservationId);

	int cancelReservationByReservationId(@Param("reservationId") Long reservationId);

	// 식당 예약 내역 QR
	QrRestaurantReservationDto findQrInfo(@Param("reservationId") Long reservationId);
}
