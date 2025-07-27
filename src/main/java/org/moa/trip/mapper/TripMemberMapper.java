package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.TripMember;

import java.util.List;

@Mapper
public interface TripMemberMapper {
    void insert(TripMember tripMember);

    List<TripMember> searchTripMembersByTripId(Long tripId);
}
