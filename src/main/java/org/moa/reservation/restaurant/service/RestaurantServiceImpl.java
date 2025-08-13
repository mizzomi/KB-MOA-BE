package org.moa.reservation.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.moa.reservation.dto.ReservationItemResponseDto;
import org.moa.reservation.restaurant.dto.*;
import org.moa.reservation.restaurant.mapper.RestaurantMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        List<AvailableTimeResponseDto> availableTimes = restaurantMapper.findAvailableSlotsByDate(restId, date);

        log.info("예약 가능 시간 조회 완료 : {}개 시간대 가능", availableTimes.size());

        return availableTimes;
    }

    // 예약 생성
    @Transactional
    @Override
    public void createReservation(Long memberId, RestaurantReservationRequestDto dto) {
        log.info("예약 생성 시작: memberId={}, request={}", memberId, dto);

        LocalDate date = LocalDate.parse(dto.getDate());

        // 1. tripDayId 조회
        Long tripDayId = restaurantMapper.findTripDayId(dto.getTripId(), date);
        if (tripDayId == null) {
            throw new RuntimeException("해당 날짜에 대한 여행 정보가 없습니다.");
        }

        // 2. dailySlotId 조회
        Long dailySlotId = restaurantMapper.findDailySlotId(dto.getRestId(), date, dto.getTime());
        if (dailySlotId == null) {
            throw new RuntimeException("예약 불가능한 시간이거나 마감되었습니다.");
        }

        // 3. 해당 슬롯의 예약 가능 인원 줄이기
        int updatedRows = restaurantMapper.decreaseCapacity(dailySlotId, dto.getResNum());
        if (updatedRows == 0) {
            throw new RuntimeException("예약 처리 중 좌석이 마감되었습니다. 다시 시도해주세요.");
        }

        // 3. RESERVATION insert
        restaurantMapper.insertReservation(tripDayId);
        Long reservationId = restaurantMapper.findLastInsertedReservationId();

        // 4. DTO로 정리해서 REST_RES insert
        RestaurantReservationInsertDto insertDto = new RestaurantReservationInsertDto();
        insertDto.setReservationId(reservationId);
        insertDto.setTripId(dto.getTripId());
        insertDto.setTripDayId(tripDayId);
        insertDto.setRestId(dto.getRestId());
        insertDto.setDailySlotId(dailySlotId);
        insertDto.setResNum(dto.getResNum());

        restaurantMapper.insertRestaurantReservation(insertDto);
        log.info("예약 생성 완료: reservationId={}", reservationId);
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

    @Override
    public List<ReservationItemResponseDto> getRestaurantReservations(Long tripId) {
        return restaurantMapper.getRestaurantReservationsByTripId(tripId);
    }

    @Override
    public List<ReservationItemResponseDto> getRestaurantReservationsByDateAndMember(Long memberId, Long tripId, java.time.LocalDate date) {
        return restaurantMapper.getRestaurantReservationsByDateAndMember(memberId, tripId, date);
    }
}
