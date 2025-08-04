package org.moa.reservation.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.reservation.restaurant.dto.*;
import org.moa.reservation.restaurant.mapper.RestaurantMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantMapper restaurantMapper;

    // 예약 가능한 식당 조회
    @Override
    public Page<RestaurantListResponseDto> getAvailableRestaurants(Long tripId, LocalDate date, String category, Pageable pageable) {

        log.info("예약 가능한 식당 조회 시작 : tripId={}, date={}, category={}, page={}", tripId, date, category, pageable.getPageNumber());

        List<RestaurantListResponseDto> restaurants =
                restaurantMapper.findAvailableRestaurant(tripId, date, category, pageable);

        int total = restaurantMapper.countAvailableRestaurants(tripId, date, category);

        log.info("예약 가능한 식당 조회 완료 : 총 {}건", total);

        return new PageImpl<>(restaurants, pageable, total);

    }

    // 식당 정보 조회
    @Override
    public RestaurantInfoResponseDto getRestaurantInfo(Long restId) {
        return restaurantMapper.findRestaurantInfo(restId);
    }

    // 예약 가능한 시간대 조회
    @Override
    public List<AvailableTimeResponseDto> getAvailableTime(Long restId, LocalDate date) {

        log.info("예약 가능 시간 조회 시작 : restId={}, date={}", restId, date);

        List<String> timeSlots = restaurantMapper.findTimeSlot(restId);

        List<AvailableTimeResponseDto> availableTimes = new ArrayList<>();

        for(String timeSlot : timeSlots) {
            Long restTimeId = restaurantMapper.findRestTimeId(restId, timeSlot);
            int max = restaurantMapper.findMaxCapacity(restTimeId);
            int reserved = restaurantMapper.findReservedCount(restTimeId);
            int available = Math.max(max - reserved, 0);

            log.debug("  - 시간 {} : 최대 {}, 예약 {}, 가능 {}", timeSlot, max, reserved, available);

            if (available > 0) {
                availableTimes.add(new AvailableTimeResponseDto(
                        timeSlot,
                        restTimeId,
                        max,
                        reserved,
                        available
                ));
            }
        }

        log.info("예약 가능 시간 조회 완료 : {}개 시간대 가능", availableTimes.size());

        return availableTimes;
    }

    // 예약 생성
    @Transactional
    @Override
    public void createReservation(Long memberId, RestaurantReservationRequestDto dto) {

        // 1. tripDayId 조회
        Long tripDayId = restaurantMapper.findTripDayId(dto.getTripId(), LocalDate.parse(dto.getDate()));

        // 2. restTimeId 조회
        Long restTimeId = restaurantMapper.findRestTimeId(dto.getRestId(), dto.getTime());

        // 3. RESERVATION insert
        restaurantMapper.insertReservation(tripDayId);
        Long reservationId = restaurantMapper.findLastInsertedReservationId();

        // 4. DTO로 정리해서 REST_RES insert
        RestaurantReservationInsertDto insertDto = new RestaurantReservationInsertDto();
        insertDto.setReservationId(reservationId);
        insertDto.setTripId(dto.getTripId());
        insertDto.setTripDayId(tripDayId);
        insertDto.setRestId(dto.getRestId());
        insertDto.setRestTimeId(restTimeId);
        insertDto.setResTime(dto.getTime());
        insertDto.setResNum(dto.getResNum());

        restaurantMapper.insertRestaurantReservation(insertDto);
    }


    // 여행별 식당 예약 조회
    @Override
    public List<RestaurantReservationResponseDto> getReservations(Long tripId) {
        return restaurantMapper.findReservations(tripId);
    }
     
    // 식당 예약 상세 조회
    @Override
    public RestaurantReservationDetailDto getReservationDetail(Long restResId) {
        return restaurantMapper.findReservationDetail(restResId);
    }
}
