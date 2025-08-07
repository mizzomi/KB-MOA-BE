package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.entity.TripMember;

import java.util.List;

@Mapper
public interface TripMemberMapper {
    void insert(TripMember tripMember);

    int existMemberInTrip(@Param("tripId") Long tripId,@Param("memberId") Long memberId);

    List<TripMember> searchTripMembersByTripId(Long tripId);

    // 사용자가 해당 여행의 멤버인지 확인
    int isMemberOfTrip(@Param("tripId")Long tripId, @Param("memberId") Long memberId);
}
