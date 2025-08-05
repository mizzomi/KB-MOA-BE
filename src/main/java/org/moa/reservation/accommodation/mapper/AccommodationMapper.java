package org.moa.reservation.accommodation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.accommodation.entity.AccomRes;
import org.moa.reservation.accommodation.entity.AccommodationInfo;
import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.trip.type.Location;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccommodationMapper {
    List<AccommodationInfo> searchAvailableAccomms(
            @Param("location") Location location,
            @Param("checkinDay") LocalDateTime checkinDay,
            @Param("checkoutDay") LocalDateTime checkoutDay,
            @Param("pageable") Pageable pageable);

    List<AccomRes> searchAvailableRooms(
            @Param("accomId") Long accomId,
            @Param("location") Location location,
            @Param("guests") Integer guests,
            @Param("checkinTime") LocalDateTime checkinTime,
            @Param("checkoutTime") LocalDateTime checkoutTime
    );

    AccommodationInfo searchAccommById(Long accomId);

    List<ReservationItemResponseDto> getAccommodationReservationsByTripId(@Param("tripId") Long tripId);
    
    List<ReservationItemResponseDto> getAccommodationReservationsByDateAndMember(
        @Param("memberId") Long memberId, 
        @Param("tripId") Long tripId, 
        @Param("date") LocalDate date);
}
