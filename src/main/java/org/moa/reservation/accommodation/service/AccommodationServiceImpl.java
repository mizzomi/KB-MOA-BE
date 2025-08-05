package org.moa.reservation.accommodation.service;

import com.beust.ah.A;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.account.dto.payment.PaymentResponseDto;
import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.account.service.AccountService;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.ResKind;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.reservation.accommodation.dto.AccommodationDetailResponse;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.reservation.accommodation.dto.AccommodationReservationRequestDto;
import org.moa.reservation.accommodation.dto.AccommodationRoomsResponse;
import org.moa.reservation.accommodation.entity.AccomRes;
import org.moa.reservation.accommodation.entity.AccommodationInfo;
import org.moa.reservation.accommodation.mapper.AccomResMapper;
import org.moa.reservation.accommodation.mapper.AccommodationMapper;
import org.moa.reservation.entity.Reservation;
import org.moa.reservation.mapper.ReservationMapper;
import org.moa.trip.entity.TripDay;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.type.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationMapper accommodationMapper;
    private final TripMapper tripMapper;
    private final ReservationMapper reservationMapper;
    private final AccomResMapper accomResMapper;
    private final AccountService accountService;

    @Override
    @Transactional
    public Page<AccommodationInfoResponse> searchAccommodations(Long tripId, LocalDate checkinDay, LocalDate checkoutDay, Pageable pageable){
        LocalDateTime checkinTime = checkinDay.atTime(15, 0);
        LocalDateTime checkoutTime = checkoutDay.atTime(11, 0);

        Location location = tripMapper.searchTripById(tripId).getTripLocation();

        // 1. ACCOMMODATION 중 남은 방이 있는 애들 AND 입실 날짜~퇴실 날짜 사이의 방들이 비어있는 애들을 페이징하여 찾기
        List<AccommodationInfo> accomms = accommodationMapper.searchAvailableAccomms(location,checkinTime,checkoutTime,pageable);
        if(accomms.isEmpty()){
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
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
    public Long reserveRoom(Long memberId, AccommodationReservationRequestDto dto){
        log.info("checkinDay : {} , checkoutDay : {}",dto.getCheckinDay(),dto.getCheckoutDay());

        BigDecimal hotelPrice = accomResMapper.searchHotelPriceById(dto.getAccomResId());
        if(hotelPrice.compareTo(dto.getPrice()) != 0) {
            throw new IllegalArgumentException("입력된 금액이 실제 결제될 금액과 다릅니다.");
        }

        Period period = Period.between(dto.getCheckinDay(), dto.getCheckoutDay());
        int nights = period.getDays();

        int availableCount = accomResMapper.checkAvailability(
                dto.getAccomResId(),
                dto.getCheckinDay(),
                dto.getCheckoutDay(),
                dto.getGuests()
        );

        if(availableCount < nights) {
            throw new IllegalArgumentException("해당 기간에 예약 가능한 방이 부족합니다. 필요: " + nights + ", 가능: " + availableCount);
        }

        // 1. trip_day 조회
        Long tripDayId = tripMapper.findTripDayId(dto.getTripId(),dto.getCheckinDay());
        if(tripDayId == null){
            throw new IllegalArgumentException("해당 날짜의 trip_day가 존재하지 않습니다.");
        }

        // 2. reservation 생성
        Reservation reservation = Reservation.builder()
                .tripDayId(tripDayId)
                .resKind(ResKind.ACCOMMODATION)
                .build();
        reservationMapper.insertReservation(reservation);
        Long reservationId = reservation.getReservationId();

        LocalDate currentDate = dto.getCheckinDay();

        while (!currentDate.isAfter(dto.getCheckoutDay().minusDays(1))) {
            // 현재 날짜의 trip_day_id 조회
            Long currentTripDayId = tripMapper.findTripDayId(dto.getTripId(), currentDate);
            if(currentTripDayId == null){
                throw new IllegalArgumentException("해당 날짜의 trip_day가 존재하지 않습니다: " + currentDate);
            }

            // 현재 날짜에 해당하는 체크인/체크아웃 시간을 가진 방 찾아서 업데이트
            LocalDateTime checkInTime = currentDate.atTime(15, 0);
            LocalDateTime checkOutTime = currentDate.plusDays(1).atTime(11, 0);

            accomResMapper.updateAccomResByDate(
                    reservationId,  // 동일한 reservationId 사용
                    currentTripDayId,  // 각 날짜별 trip_day_id 사용
                    dto.getAccomResId(),
                    dto.getGuests(),
                    checkInTime,
                    checkOutTime
            );

            currentDate = currentDate.plusDays(1);
        }

        // 4. 결제 로직
        String hotelName = accomResMapper.searchHotelNameByAccomResId(dto.getAccomResId());
        PaymentResponseDto result = accountService.makePayment(memberId, dto.getPrice(), hotelName);

        return reservationId;
    }
}
