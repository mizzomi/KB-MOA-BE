package org.moa.global.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.handler.BusinessException;
import org.moa.global.notification.dto.NotificationResponseDto;
import org.moa.global.notification.dto.SettleNotificationRequestDto;
import org.moa.global.notification.dto.TripNotificationRequestDto;
import org.moa.global.notification.entity.Notification;
import org.moa.global.notification.mapper.NotificationMapper;
import org.moa.global.type.NotificationType;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripMember;
import org.moa.trip.mapper.SettlementMapper;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.mapper.TripMemberMapper;
import org.moa.trip.type.TripRole;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final MemberMapper memberMapper;
    private final NotificationMapper notificationMapper;
    private final TripMapper tripMapper;
    private final TripMemberMapper tripMemberMapper;
    private final SettlementMapper settlementMapper;

    @Override
    @Transactional
    public List<NotificationResponseDto> getNotifications(){
        // 1. 현재 로그인된 사용자 정보 조회
        UserDetails userDetails = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                userDetails = (UserDetails) principal;
            } else {
                // UserDetails 타입이 아닌 경우 (예: 인증되지 않은 사용자)
                log.warn("인증된 사용자 정보가 UserDetails 타입이 아닙니다.");
                throw new BusinessException(StatusCode.UNAUTHORIZED);
            }
        } catch (Exception e) {
            // SecurityContextHolder에서 Principal을 가져오지 못한 경우 (인증 실패)
            log.error("인증된 사용자 정보를 가져오는 데 실패했습니다.", e);
            throw new BusinessException(StatusCode.UNAUTHORIZED);
        }
        // 2. Member 정보 조회
        Member member;
        try {
            member = memberMapper.getByEmail(userDetails.getUsername());
            if (member == null) {
                log.warn("사용자 이메일({})에 해당하는 회원 정보를 찾을 수 없습니다.", userDetails.getUsername());
                throw new BusinessException(StatusCode.BAD_REQUEST, "해당하는 사용자 이메일의 유저가 없습니다.");
            }
        } catch (DataAccessException e) {
            log.error("회원 정보 조회 중 데이터베이스 오류 발생", e);
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "회원 정보 조회 중 오류가 발생했습니다.");
        }

        // 3. 알림 목록 조회
        List<Notification> notifications;
        try {
            notifications = notificationMapper.searchNotificationsByMemberIdAndUnread(member.getMemberId());
        } catch (DataAccessException e) {
            log.error("알림 목록 조회 중 데이터베이스 오류 발생", e);
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "알림 목록 조회 중 오류가 발생했습니다.");
        }

        // 4. 알림이 없을 경우 빈 리스트 반환
        if (notifications == null || notifications.isEmpty()) {
            log.info("읽지 않은 알림이 없습니다. memberId: {}", member.getMemberId());
            return new ArrayList<>();
        }

        // 5. Notification 객체를 DTO 로 변환
        List<NotificationResponseDto> notificationResponseDtos = new ArrayList<>();
        for (Notification notification : notifications) {
            NotificationResponseDto notificationResponseDto = NotificationResponseDto.builder()
                    .notificationId(notification.getNotificationId())
                    .tripId(notification.getTripId())
                    .expenseId(notification.getExpenseId())
                    .notificationType(notification.getNotificationType())
                    .sender(notification.getSenderName())
                    .tripName(notification.getTripName())
                    .build();
            notificationResponseDtos.add(notificationResponseDto);
        }

        log.info("총 {}개의 알림을 성공적으로 조회했습니다.", notificationResponseDtos.size());
        return notificationResponseDtos;
    };

    @Override
    @Transactional
    public boolean tripNotification(TripNotificationRequestDto dto){
        log.info("여행 알림 처리 메서드 호출: dto = {}", dto);

        // 1. 현재 로그인된 사용자 정보 조회
        UserDetails userDetails = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                userDetails = (UserDetails) principal;
            } else {
                log.warn("인증된 사용자 정보가 UserDetails 타입이 아닙니다.");
                throw new BusinessException(StatusCode.UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("인증된 사용자 정보를 가져오는 데 실패했습니다.", e);
            throw new BusinessException(StatusCode.UNAUTHORIZED);
        }

        // 2. Member 정보 조회
        Member member;
        try {
            member = memberMapper.getByEmail(userDetails.getUsername());
            if (member == null) {
                log.warn("사용자 이메일({})에 해당하는 회원 정보를 찾을 수 없습니다.", userDetails.getUsername());
                throw new BusinessException(StatusCode.BAD_REQUEST, "해당하는 사용자 이메일의 유저가 없습니다.");
            }
        } catch (DataAccessException e) {
            log.error("회원 정보 조회 중 데이터베이스 오류 발생", e);
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "회원 정보 조회 중 오류가 발생했습니다.");
        }
        log.info("member : {} ",member);
        // 3. 여행 초대 수락/거절 처리
        if ("수락".equals(dto.getType())) { // 문자열 비교는 .equals() 사용 권장
            Trip trip;
            try {
                trip = tripMapper.searchTripById(dto.getTripId());
                if (trip == null) {
                    log.warn("유효하지 않은 tripId({})로 여행 수락 요청이 들어왔습니다.", dto.getTripId());
                    throw new BusinessException(StatusCode.BAD_REQUEST, "유효하지 않은 여행 ID입니다.");
                }
            } catch (DataAccessException e) {
                log.error("여행 정보 조회 중 데이터베이스 오류 발생", e);
                throw new BusinessException(StatusCode.INTERNAL_ERROR, "여행 정보 조회 중 오류가 발생했습니다.");
            }

            // TripMember에 나를 MEMBER로 추가
            try {
                TripMember tripMember = TripMember.builder()
                        .tripId(trip.getTripId())
                        .memberId(member.getMemberId())
                        .role(TripRole.MEMBER)
                        .joinedAt(LocalDateTime.now())
                        .build();
                tripMemberMapper.insert(tripMember);
                log.info("사용자({})가 여행({})에 성공적으로 합류했습니다.", member.getMemberId(), trip.getTripId());
            } catch (DataAccessException e) {
                log.error("TripMember 추가 중 데이터베이스 오류 발생", e);
                throw new BusinessException(StatusCode.INTERNAL_ERROR, "여행 합류 처리 중 오류가 발생했습니다.");
            }
        } else if ("거절".equals(dto.getType())) { // 거절 로직도 명시적으로 처리
            log.info("사용자({})가 여행 초대({})를 거절했습니다.", member.getMemberId(), dto.getTripId());
            // 필요하다면 거절에 대한 추가 로직 (예: TripMember에서 제거, 초대 상태 변경 등)
        } else {
            log.warn("알 수 없는 알림 타입({})으로 요청이 들어왔습니다.", dto.getType());
            throw new BusinessException(StatusCode.BAD_REQUEST, "유효하지 않은 알림 타입입니다.");
        }
        // 4. Notification 을 읽음 처리
        return readNotification(dto.getNotificationId());
    }

    @Override
    @Transactional
    public boolean settleNotification(SettleNotificationRequestDto dto){
        log.info("정산 알림 처리 메서드 호출: dto = {}", dto);

        // 1. 현재 로그인된 사용자 정보 조회
        UserDetails userDetails = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                userDetails = (UserDetails) principal;
            } else {
                log.warn("인증된 사용자 정보가 UserDetails 타입이 아닙니다.");
                throw new BusinessException(StatusCode.UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("인증된 사용자 정보를 가져오는 데 실패했습니다.", e);
            throw new BusinessException(StatusCode.UNAUTHORIZED);
        }

        // 2. Member 정보 조회
        Member member;
        try {
            member = memberMapper.getByEmail(userDetails.getUsername());
            if (member == null) {
                log.warn("사용자 이메일({})에 해당하는 회원 정보를 찾을 수 없습니다.", userDetails.getUsername());
                throw new BusinessException(StatusCode.BAD_REQUEST, "해당하는 사용자 이메일의 유저가 없습니다.");
            }
        } catch (DataAccessException e) {
            log.error("회원 정보 조회 중 데이터베이스 오류 발생", e);
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "회원 정보 조회 중 오류가 발생했습니다.");
        }
        return readNotification(dto.getNotificationId());
    }

    public boolean readNotification(Long notificationId){
        try {
            notificationMapper.readNotification(notificationId);
            log.info("알림({})을 읽음 처리했습니다.", notificationId);
        } catch (DataAccessException e) {
            log.error("알림 읽음 처리 중 데이터베이스 오류 발생", e);
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "알림 처리 중 오류가 발생했습니다.");
        }

        log.info("여행 알림 처리 완료.");
        return true;
    }
}
