package org.moa.reservation.transport.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.transport.dto.TransResStatusDto;
import org.moa.reservation.transport.dto.TranstInfoResponse;
import org.moa.reservation.transport.dto.TransSeatsInfoResponse;

@Mapper
public interface TransportMapper {
	// 페이징 조회용: offset, limit
	List<TranstInfoResponse> selectTransports(
		@Param("departureName")     String        departureName,
		@Param("destinationName")   String        destinationName,
		@Param("departureDateTime") LocalDateTime departureDateTime,
		@Param("offset")            int           offset,
		@Param("limit")             int           limit
	);

	// 전체 건수 조회용
	int countTransports(
		@Param("departureName")     String        departureName,
		@Param("destinationName")   String        destinationName,
		@Param("departureDateTime") LocalDateTime departureDateTime
	);

	List<TransSeatsInfoResponse> selectSeatsByTransportId(
		@Param("transportId") Long transportId
	);

	List<TransResStatusDto> findStatusesByIds(@Param("list") List<Long> tranResIds);

	int updateSeatsToPending(
		@Param("reservationId") Long reservationId,
		@Param("tripDayId") Long tripDayId,
		@Param("tranResIds") List<Long> tranResIds,
		@Param("bookedAt") LocalDateTime bookedAt);

	BigDecimal getTotalPriceByReservationId(@Param("reservationId") Long reservationId);

	String selectTrainNoByReservationId(@Param("reservationId") Long reservationId);

	int confirmSeatsByReservationId(@Param("reservationId") Long reservationId);

	int cancelSeatsByReservationId(@Param("reservationId") Long reservationId);

	List<Long> findExistingTranResIds(@Param("tranResIds") List<Long> tranResIds);

	List<ReservationItemResponseDto> getTransportReservationsByTripId(@Param("tripId") Long tripId);
	
	List<ReservationItemResponseDto> getTransportReservationsByDateAndMember(
		@Param("memberId") Long memberId, 
		@Param("tripId") Long tripId, 
		@Param("date") java.time.LocalDate date);
}
