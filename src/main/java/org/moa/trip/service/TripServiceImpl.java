package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.notification.entity.Notification;
import org.moa.global.notification.mapper.NotificationMapper;
import org.moa.global.service.FcmService;
import org.moa.global.type.NotificationType;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.dto.trip.*;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripDay;
import org.moa.trip.entity.TripLocation;
import org.moa.trip.entity.TripMember;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.mapper.TripMemberMapper;
import org.moa.trip.type.Location;
import org.moa.trip.type.TripRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
    private final FcmService fcmService;

    private final TripMapper tripMapper;
    private final TripMemberMapper tripMemberMapper;
    private final MemberMapper memberMapper;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public Long createTrip(TripCreateRequestDto dto){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberMapper.getByEmail(userDetails.getUsername());

        log.info("createTrip : DTO = {}",dto);
        Location location = Location.valueOf(dto.getLocation().toUpperCase());
        Trip newTrip = Trip.builder()
                .memberId(member.getMemberId()) // 여행 생성자 ID
                .tripName(dto.getTripName())
                .tripLocation(location)
                .startDate(dto.getStartTime().toLocalDate())
                .endDate(dto.getEndTime().toLocalDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tripMembers(new ArrayList<>())
                .tripRecords(new ArrayList<>())
                .expenses(new ArrayList<>())
                .tripDays(new ArrayList<>())
                .build();
        // 여행을 DB에 저장
        tripMapper.insert(newTrip);
        
        // 호스트를 생성
        TripMember host  = TripMember.builder()
                .tripId(newTrip.getTripId())
                .memberId(newTrip.getMemberId())
                .role(TripRole.HOST)
                .joinedAt(LocalDateTime.now())
                .build();

        // 호스트 참여를 DB에 저장
        tripMemberMapper.insert(host);

        // 3. startDate ~ endDate 사이의 TripDay 생성 및 DB 저장
        List<TripDay> tripDaysToInsert = new ArrayList<>();
        LocalDate startDate = dto.getStartTime().toLocalDate();
        LocalDate endDate = dto.getEndTime().toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (int i = 0; i < daysBetween; i++) {
            TripDay tripDay = TripDay.builder()
                    .tripId(newTrip.getTripId())
                    .day(startDate.plusDays(i))
                    .build();
            tripDaysToInsert.add(tripDay);
        }
        tripMapper.insertTripDays(tripDaysToInsert); // 배치 insert 메서드 호출


        // 참여자들에게 알림 생성
        if(dto.getMemberIds() != null){
            for(Long memberId : dto.getMemberIds()){
                String title = "여행 초대 요청";
                String content = member.getName() +"님이 \"" + dto.getTripName() + "\" 여행에 초대하셨습니다.";
                // 알림 생성
                Notification notification = Notification.builder()
                        .memberId(memberId)
                        .tripId(newTrip.getTripId())
                        .notificationType(NotificationType.TRIP)
                        .senderName(member.getName())
                        .tripName(dto.getTripName())
                        .title(title)
                        .content(content)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                // 유저들에게 여행 초대 알림 보내는 서비스 로직
                fcmService.sendNotification(memberId,title,content);
                // 알림을 DB에 생성하는 로직
                notificationMapper.createNotification(notification);
            }
        }
        return newTrip.getTripId();
    }

    @Override
    @Transactional
    public boolean addMemberToTrip(AddMemberRequestDto dto){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member sender = memberMapper.getByEmail(userDetails.getUsername());

        Trip trip = tripMapper.searchTripById(dto.getTripId());

        String title = "여행 초대 요청";
        String content = sender.getName() +"님이 \"" + trip.getTripName() + "\" 여행에 초대하셨습니다.";
        // 알림 생성
        Notification notification = Notification.builder()
                .memberId(dto.getMemberId())
                .tripId(trip.getTripId())
                .notificationType(NotificationType.TRIP)
                .senderName(sender.getName())
                .tripName(trip.getTripName())
                .title(title)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 유저들에게 여행 초대 알림 보내는 서비스 로직
        fcmService.sendNotification(dto.getMemberId(),title,content);
        // 알림을 DB에 생성하는 로직
        notificationMapper.createNotification(notification);
        return true;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TripListResponseDto> getTripList(Long memberId, String locationName, Pageable pageable) {

        List<TripListResponseDto> trips = tripMapper.findTripsByMemberId(memberId, locationName, pageable);
        int total = tripMapper.countTripsByMemberId(memberId, locationName);

        return new PageImpl<>(trips, pageable, total);
    }

    @Override
    @Transactional
    public List<TripLocationResponseDto> getTripLocations(){
        List<TripLocation> tripLocations = tripMapper.searchTripLocations();
        List<TripLocationResponseDto> tripLocationResponseDtos = new ArrayList<>();
        for(TripLocation tripLocation : tripLocations){
            log.info("{}",tripLocation.getLocationName());
            log.info("{}",tripLocation.getLocationName().toString());
            TripLocationResponseDto tripLocationResponseDto = TripLocationResponseDto.builder()
                    .locationName(tripLocation.getLocationName())
                    .longitude(tripLocation.getLongitude())
                    .latitude(tripLocation.getLatitude())
                    .address(tripLocation.getAddress())
                    .build();
            tripLocationResponseDtos.add(tripLocationResponseDto);
        }
        return tripLocationResponseDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public TripDetailResponseDto getTripDetail(Long tripId) {
        log.info("여행 상세 조회 시작 - tripId: {}", tripId);
        
        Trip trip = tripMapper.searchTripById(tripId);
        if (trip == null) {
            throw new IllegalArgumentException("해당 ID의 여행을 찾을 수 없습니다: " + tripId);
        }
        
        // 여행 상태 판단 로직
        String status = determineStatus(trip.getStartDate(), trip.getEndDate());
        
        TripDetailResponseDto response = TripDetailResponseDto.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .startDate(trip.getStartDate().toString())
                .endDate(trip.getEndDate().toString())
                .locationName(trip.getTripLocation().toString())
                .status(status)
                .build();
        
        log.info("여행 상세 조회 완료 - tripId: {}, tripName: {}", tripId, trip.getTripName());
        return response;
    }
    
    private String determineStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (today.isBefore(startDate)) {
            return "여행예정";
        } else if (today.isAfter(endDate)) {
            return "여행완료";
        } else {
            return "여행중";
        }
    }
}
