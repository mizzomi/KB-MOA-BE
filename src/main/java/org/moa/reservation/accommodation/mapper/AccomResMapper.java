package org.moa.reservation.accommodation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface AccomResMapper {
   void updateAccomRes(@Param("reservationId") Long reservationId,
                        @Param("tripDayId") Long tripDayId,
                        @Param("guests") Integer guests,
                        @Param("accomResId") Long accomResId,
                        @Param("checkinDay") LocalDate checkinDay,
                        @Param("checkoutDay") LocalDate checkoutDay,
                        @Param("nights") Integer nights); // 숙박 일수(nights) 파라미터 추가
   String searchHotelNameByAccomResId(@Param("accomResId") Long accomResId);
   BigDecimal searchHotelPriceById(@Param("accomResId")  Long accomResId);
}
