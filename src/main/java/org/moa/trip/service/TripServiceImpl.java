package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripMember;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.mapper.TripMemberMapper;
import org.moa.trip.type.Location;
import org.moa.trip.type.TripRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
    private final TripMapper tripMapper;
    private final TripMemberMapper tripMemberMapper;

    @Override
    @Transactional
    public boolean createTrip(TripCreateRequestDto dto){
        log.info("createTrip : DTO = {}",dto);
        Location location = Location.valueOf(dto.getLocation().toUpperCase());
        Trip newTrip = Trip.builder()
                .memberId(dto.getMemberId()) // 여행 생성자 ID
                .tripName(dto.getTripName())
                .startDate(dto.getStartTime())
                .endDate(dto.getEndTime())
                // DTO location String 필드를 대문자로 바꾼뒤 Location.valueOf() 사용
                .tripLocation(location)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .tripMembers(new ArrayList<>())
                .tripRecords(new ArrayList<>())
                .expenses(new ArrayList<>())
                .tripDays(new ArrayList<>())
                .build();
        // 호스트를 생성 / DB에 저장
        TripMember host  = TripMember.builder()
                .tripId(newTrip.getTripId())
                .memberId(newTrip.getMemberId())
                .role(TripRole.HOST)
                .joinedAt(LocalDateTime.now())
                .build();
        tripMemberMapper.insert(host);

        // 호스트를 본인 여행에 추가 / 여행을 DB에 저장
        newTrip.getTripMembers().add(host);
        tripMapper.insert(newTrip);

        // 참여자들에게 알림 생성
        if(dto.getMemberIds() != null){
            for(Long memberId : dto.getMemberIds()){
                // !여기서 추후 유저들에게 여행 초대 알림 보내는 서비스 로직 필요! ex) NotificationService 등
            }
        }
        // !추후 초대받은 유저들이 수락시 newTrip 에 해당 유저들 추가하는 서비스 로직 필요!
        return true;
    }
}
