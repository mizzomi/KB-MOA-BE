package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.TripRecordImage;

@Mapper
public interface TripRecordImageMapper {
    void insertTripRecordImage(TripRecordImage recordImage);

}
