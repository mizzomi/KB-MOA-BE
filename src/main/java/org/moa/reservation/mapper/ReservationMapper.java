package org.moa.reservation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.entity.Reservation;

@Mapper
public interface ReservationMapper {
	void insertReservation(Reservation reservation);

	Long findMemberIdByReservationId(@Param("reservationId") Long reservationId);
}
