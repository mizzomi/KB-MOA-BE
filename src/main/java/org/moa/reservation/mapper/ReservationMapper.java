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

	// 식당 예약
	UserRestaurantReservationDto findRestInfoByRestResId(@Param("restResId") Long restResId);
	List<UserRestaurantReservationDto> findUserRestInfoByReservationId(@Param("reservationId") Long reservationId);

	// 숙박 예약
	UserAccommodationReservationDto findAccomInfoByAccomResId(@Param("accomResId") Long accomResId);
	List<UserAccommodationReservationDto> findUserAccomInfoByReservationId(@Param("reservationId") Long reservationId);
	
	// 교통 예약
	UserTransportReservationDto findTransInfoByTranResId(@Param("tranResId") Long tranResId);
	List<UserTransportReservationDto> findUserTransInfoByReservationId(@Param("reservationId") Long reservationId);

	// QR 권한 검증
	boolean isTripMemberByReservationIdAndMemberId(@Param("reservationId") Long reservationId,
												   @Param("memberId") Long memberId);

	// 사장님 사업장 소유권 검증
	boolean isOwnerOfBusiness(@Param("ownerId")Long ownerId,
							  @Param("businessId") Long businessId,
							  @Param("businessType") String businessType);
}