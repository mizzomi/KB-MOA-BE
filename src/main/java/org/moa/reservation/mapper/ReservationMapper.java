package org.moa.reservation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.dto.qr.*;
import org.moa.reservation.entity.Reservation;

import java.util.List;

@Mapper
public interface ReservationMapper {
	void insertReservation(Reservation reservation);

	Long findMemberIdByReservationId(@Param("reservationId") Long reservationId);

	int cancelReservationByReservationId(@Param("reservationId") Long reservationId);

	// 식당 / 숙박 / 교통
	String findTypeByReservationId(@Param("reservationId") Long reservationId);

	// 식당 예약 내역 QR
	QrRestaurantReservationDto findRestQrInfoByReservationId(@Param("reservationId") Long reservationId);

	// 식당 예약 내역 정보
	UserRestaurantReservationDto findUserRestInfoByReservationId(@Param("reservationId") Long reservationId);

	// 숙박 예약 내역 QR
	QrAccommodationReservationDto findAccomQrInfoByReservationId(@Param("reservationId") Long reservationId);
	
	// 숙박 예약 내역 정보
	UserAccommodationReservationDto findUserAccomInfoByReservationId(@Param("reservationId") Long reservationId);
	
	// 교통 예약 내역 QR
	QrTransportReservationDto findTransQrInfoByTranResId(@Param("tranResId") Long tranResId);
	
	// 교통 예약 내역 정보
	List<UserTransportReservationDto> findUserTransInfoByReservationId(@Param("reservationId") Long reservationId);

	// QR 권한 검증
	boolean isTripMemberByReservationIdAndMemberId(@Param("reservationId") Long reservationId,
												   @Param("memberId") Long memberId);

	// 사장님 사업장 소유권 검증
	boolean isOwnerOfBusiness(@Param("ownerId")Long ownerId,
							  @Param("businessId") Long businessId,
							  @Param("businessType") String businessType);
}