package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.global.config.RootConfig;
import org.moa.global.config.ServletConfig;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.member.type.MemberRole;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Log4j2
@Transactional
class TripServiceImplTest {
    @Autowired
    private TripService tripService;

    @Autowired
    private MemberMapper memberMapper;

    private TripCreateRequestDto dto;
    private Long hostMemberId = 1L;

    @BeforeEach
    void setUp(){
        Long id = insertTemporaryMembers(hostMemberId);

        dto = TripCreateRequestDto.builder()
                .memberId(id)
                .tripName("부산 여행")
                .startTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .endTime(LocalDateTime.of(2025, 8, 5, 18, 0))
                .location("BUSAN")
                .memberIds(new ArrayList<>())
                .build();
    }


    private Long insertTemporaryMembers(Long memberId) {
        try {
            // Member 엔티티를 생성하고 데이터를 설정합니다.
            // MemberMapper.xml의 insert 쿼리에 맞춰 모든 NOT NULL 필드를 채워야 합니다.
            Member member = Member.builder()
                    .memberType(MemberRole.ROLE_USER)
                    .email("test"+memberId+"@naver.com")
                    .password("hash password test"+memberId)
                    .name("Test"+memberId+"User")
                    .idCardNumber("1234561234567"+memberId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            // MemberMapper의 insert 메서드를 호출하여 DB에 삽입합니다.
            memberMapper.insert(member);
            log.info("Temporary member {} inserted for test using MemberMapper.", memberId);
            return member.getMemberId();
        } catch (Exception e) {
            // 이미 존재하는 경우 등, 에러가 발생해도 테스트를 계속 진행하기 위해 처리
            // 실제 테스트에서는 오류 발생 시 테스트가 실패하도록 RuntimeException을 던지는 것이 좋습니다.
            log.error("Failed to insert temporary member {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to setup test member: " + memberId, e);
        }
    }

//    @Test
//    void createTrip() {
//        boolean result = tripService.createTrip(dto);
//        log.info("여행 생성 결과: {}", result);
//        assertTrue(result);
//    }
}