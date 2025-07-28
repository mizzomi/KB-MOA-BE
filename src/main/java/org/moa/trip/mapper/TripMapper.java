package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.entity.Trip;

import java.util.List;

@Mapper
public interface TripMapper {
    public Trip selectTripById(long tripId);

    public void insert(Trip trip);

    List<TripListResponseDto> getTripsByMemberIdPaged(@Param("memberId") Long memberId,
                                                      @Param("offset") int offset,
                                                      @Param("size") int size);

    int countTripsByMemberId(@Param("memberId") Long memberId);
}
