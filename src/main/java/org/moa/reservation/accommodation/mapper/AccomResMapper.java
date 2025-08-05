package org.moa.reservation.accommodation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.accommodation.entity.AccomRes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccomResMapper {
   Long searchAccomIdByAccomResId(@Param("accomResId") Long accomResId);
   void updateAccomResByDate(@Param("reservationId") Long reservationId,
                             @Param("tripDayId") Long tripDayId,
                             @Param("accomResId") Long accomResId,
                             @Param("guests") Integer guests,
                             @Param("checkInTime") LocalDateTime checkInTime,
                             @Param("checkOutTime") LocalDateTime checkOutTime);
   int checkAvailability(@Param("accomResId")Long accomResId, @Param("checkInDate")LocalDate checkInDate, @Param("checkOutDate")LocalDate checkOutDate, @Param("guests") Integer guests);
   String searchHotelNameByAccomResId(@Param("accomResId") Long accomResId);
   BigDecimal searchHotelPriceById(@Param("accomResId")  Long accomResId);
}
