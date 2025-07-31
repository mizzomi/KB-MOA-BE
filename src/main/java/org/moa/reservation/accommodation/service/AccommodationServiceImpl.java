package org.moa.reservation.accommodation.service;

import com.beust.ah.A;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.reservation.accommodation.dto.AccommodationDetailResponse;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.reservation.accommodation.dto.AccommodationReservationRequestDto;
import org.moa.reservation.accommodation.dto.AccommodationRoomsResponse;
import org.moa.reservation.accommodation.entity.AccomRes;
import org.moa.reservation.accommodation.entity.AccommodationInfo;
import org.moa.reservation.accommodation.mapper.AccommodationMapper;
import org.moa.trip.entity.TripDay;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.type.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationMapper accommodationMapper;
    private final TripMapper tripMapper;

    @Override
    @Transactional
    public Page<AccommodationInfoResponse> searchAccommodations(Long tripId, LocalDate checkinDay, LocalDate checkoutDay, Pageable pageable){
        LocalDateTime checkinTime = checkinDay.atTime(15, 0);
        LocalDateTime checkoutTime = checkoutDay.atTime(11, 0);

        Location location = tripMapper.searchTripById(tripId).getTripLocation();

        // 1. ACCOMMODATION 중 남은 방이 있는 애들 AND 입실 날짜~퇴실 날짜 사이의 방들이 비어있는 애들을 페이징하여 찾기
        List<AccommodationInfo> accomms = accommodationMapper.searchAvailableAccomms(location,checkinTime,checkoutTime,pageable);
        if(accomms.isEmpty()){
            throw new BusinessException(StatusCode.BAD_REQUEST,"해당 날짜에 빈 방이 있는 숙박시설이 없습니다");
        }
        for(AccommodationInfo accomm : accomms){
            log.info("id : {} , name : {}, address : {}, location : {}"
                    ,accomm.getAccomId()
                    ,accomm.getHotelName()
                    ,accomm.getAddress()
                    ,accomm.getLocation()
                    );
        }
        int total = accomms.size();

        List<AccommodationInfoResponse> accommodationInfoResponses = new ArrayList<>();
        for(AccommodationInfo accomm : accomms){
            AccommodationInfoResponse accommResponse = AccommodationInfoResponse.builder()
                    .accomId(accomm.getAccomId())
                    .hotelName(accomm.getHotelName())
                    .address(accomm.getAddress())
                    .location(accomm.getLocation())
                    .hotelImageUrl(accomm.getHotelImageUrl())
                    .build();
            accommodationInfoResponses.add(accommResponse);
        }

        return new PageImpl<>(accommodationInfoResponses, pageable, total);
    }

    @Override
    @Transactional
    public AccommodationDetailResponse getAccommodation(Long accomId){
        AccommodationInfo accommodationInfo = accommodationMapper.searchAccommById(accomId);
        return AccommodationDetailResponse.builder()
                .accomId(accommodationInfo.getAccomId())
                .hotelName(accommodationInfo.getHotelName())
                .address(accommodationInfo.getAddress())
                .hotelImageUrl(accommodationInfo.getHotelImageUrl())
                .build();
    }

    @Override
    @Transactional
    public List<AccommodationRoomsResponse>  getRooms(Long accomId, Long tripId, LocalDate checkinDay, LocalDate checkoutDay, Integer guests){
        LocalDateTime checkinTime = checkinDay.atTime(15, 0);
        LocalDateTime checkoutTime = checkoutDay.atTime(11, 0);

        Location location = tripMapper.searchTripById(tripId).getTripLocation();

        List<AccomRes> accomResList = accommodationMapper.searchAvailableRooms(accomId, location, guests, checkinTime, checkoutTime);
        List<AccommodationRoomsResponse>  accommodationRoomsResponses = new ArrayList<>();
        for(AccomRes accomRes : accomResList){
            AccommodationRoomsResponse accommodationRoomsResponse = AccommodationRoomsResponse.builder()
                    .accomResId(accomRes.getAccomResId())
                    .hotelName(accomRes.getHotelName())
                    .roomType(accomRes.getRoomType())
                    .roomImageUrl(accomRes.getRoomImageUrl())
                    .maxGuests(accomRes.getMaxGuests())
                    .price(accomRes.getPrice())
                    .build();
            accommodationRoomsResponses.add(accommodationRoomsResponse);
        }
        return accommodationRoomsResponses;
    }

    @Override
    @Transactional
    public Long reserveRoom(AccommodationReservationRequestDto dto){
        // 1. trip_day 조회
        Long tripDayId = tripMapper.findTripDayId(dto.getTripId(),dto.getCheckinDay());
        if(tripDayId == null){
            throw new IllegalArgumentException("해당 날짜의 trip_day가 존재하지 않습니다.");
        }

        // 2. reservation 생성
        // 3. Accom_res (방 정보) 수정
        // 4. Accommodation_info 남은 방수 최신화
        // 5. 결제 로직!

        return null;
    }
}
