package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.Trip;

@Mapper
public interface TripMapper {
    public Trip searchTripById(long tripId);

    public void insert(Trip trip);
}
