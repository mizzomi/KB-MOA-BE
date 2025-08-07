package org.moa.reservation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.dto.QrAccommodationReservationDto;
import org.moa.reservation.dto.QrRestaurantReservationDto;
import org.moa.reservation.dto.QrTransportReservationDto;
import org.moa.reservation.entity.Reservation;

@Mapper
public interface ReservationMapper {
	void insertReservation(Reservation reservation);

	Long findMemberIdByReservationId(@Param("reservationId") Long reservationId);

	int cancelReservationByReservationId(@Param("reservationId") Long reservationId);

	// 식당 / 숙박 / 교통
	String findTypeByReservationId(@Param("reservationId") Long reservationId);

	// 식당 예약 내역 QR
	QrRestaurantReservationDto findRestQrInfoByReservationId(@Param("reservationId") Long reservationId);

	// 숙박 예약 내역 QR
	QrAccommodationReservationDto findAccomQrInfoByReservationId(@Param("reservationId") Long reservationId);

	// 교통 예약 내역 QR
	QrTransportReservationDto findTransQrInfoByReservationId(@Param("reservationId") Long reservationId);

	// QR 권한 검증
	boolean isTripMemberByReservationIdAndMemberId(@Param("reservationId") Long reservationId,
												   @Param("memberId") Long memberId);

	// 사장님 식당 검증
	boolean isOwnerOfBusiness(Long ownerId, Long businessId, String businessType);
}
