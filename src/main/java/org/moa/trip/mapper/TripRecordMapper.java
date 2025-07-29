package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripRecord;

@Mapper
public interface TripRecordMapper {
    void insertTripRecord(TripRecord record);
    TripRecord findRecordById(Long recordId);
}
