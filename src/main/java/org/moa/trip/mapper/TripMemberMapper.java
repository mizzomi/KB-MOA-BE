package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.TripMember;

@Mapper
public interface TripMemberMapper {
    public void insert(TripMember tripMember);
}
