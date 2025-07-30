package org.moa.reservation.accommodation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.accommodation.entity.AccommodationInfo;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccommodationMapper {
    List<AccommodationInfo> searchAvailableAccomms(
            @Param("checkinDay") LocalDateTime checkinDay,
            @Param("checkoutDay") LocalDateTime checkoutDay,
            @Param("pageable") Pageable pageable);
}
