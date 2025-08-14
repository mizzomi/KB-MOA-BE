package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.dto.trip.TripListResponseDto;
import org.moa.trip.dto.trip.UpcomingTripResponseDto;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripDay;
import org.moa.trip.entity.TripLocation;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TripMapper {
    public Trip searchTripById(long tripId);

    public void insert(Trip trip);

    List<TripListResponseDto> findTripsByMemberId(@Param("memberId") Long memberId,
                                                  @Param("locationName") String locationName,
                                                  @Param("pageable") Pageable pageable);

    int countTripsByMemberId(@Param("memberId") Long memberId,
                             @Param("locationName") String locationName);

    List<LocalDateTime> searchDayByTripId(Long tripId);

    void insertTripDays(@Param("tripDays")List<TripDay> tripDays);

    Long findTripDayId(@Param("tripId") Long tripId, @Param("day") LocalDate departureDate);

    List<TripLocation> searchTripLocations();

    List<Long> searchIdsByDays(@Param("recordDays")List<LocalDateTime> recordDays);

    List<TripDay> searchByTripId(@Param("tripId") Long tripId);

    UpcomingTripResponseDto findUpcomingTripByMemberId(@Param("memberId") Long memberId);
}
