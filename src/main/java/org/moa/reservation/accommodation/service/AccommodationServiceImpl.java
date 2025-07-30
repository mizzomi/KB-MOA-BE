package org.moa.reservation.accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.reservation.accommodation.dto.AccommodationInfoResponse;
import org.moa.reservation.accommodation.entity.AccommodationInfo;
import org.moa.reservation.accommodation.mapper.AccommodationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationMapper accommodationMapper;

    @Override
    @Transactional
    public Page<AccommodationInfoResponse> searchAccommodations(LocalDateTime checkinDay, LocalDateTime checkoutDay, Pageable pageable){
        // 1. ACCOMMODATION 중 남은 방이 있는 애들 AND 입실 날짜~퇴실 날짜 사이의 방들이 비어있는 애들을 페이징하여 찾기
        List<AccommodationInfo> accomms = accommodationMapper.searchAvailableAccomms(checkinDay,checkoutDay,pageable);
        int total = accomms.size();

        List<AccommodationInfoResponse> accommodationInfoResponses = new ArrayList<>();
        for(AccommodationInfo accomm : accomms){
            AccommodationInfoResponse accommResponse = AccommodationInfoResponse.builder()
                    .accomId(accomm.getAccommId())
                    .hotelName(accomm.getHotelName())
                    .address(accomm.getAddress())
                    .hotelImageUrl(accomm.getHotelImageUrl())
                    .build();
            accommodationInfoResponses.add(accommResponse);
        }

        return new PageImpl<>(accommodationInfoResponses, pageable, total);
    }
}
