package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.entity.Trip;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Mapper
public interface TripMapper {
    public Trip searchTripById(long tripId);

    public void insert(Trip trip);

    List<TripListResponseDto> findTripsByMemberId(@Param("memberId") Long memberId,
                                                      @Param("pageable") Pageable pageable);

    int countTripsByMemberId(@Param("memberId") Long memberId);
}
