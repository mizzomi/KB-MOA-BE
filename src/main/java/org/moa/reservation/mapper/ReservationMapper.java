package org.moa.reservation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.reservation.entity.Reservation;

@Mapper
public interface ReservationMapper {
	void insertReservation(Reservation reservation);
}
