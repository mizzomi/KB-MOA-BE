package org.moa.reservation.accommodation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccomResMapper {
    void updateAccomRes(@Param("reservationId") Long reservationId,@Param("tripDayId") Long tripDayId,@Param("guests") Integer guests);
}
