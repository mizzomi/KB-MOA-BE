package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.Trip;

import java.util.List;

@Mapper
public interface TripMapper {
    public Trip selectTripById(long tripId);

    public void insert(Trip trip);
}
