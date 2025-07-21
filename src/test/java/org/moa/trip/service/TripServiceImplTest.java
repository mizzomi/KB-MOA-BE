package org.moa.trip.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moa.global.config.RootConfig;
import org.moa.global.config.ServletConfig;
import org.moa.trip.dto.trip.TripCreateRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, ServletConfig.class})
@Log4j2
@Transactional
class TripServiceImplTest {
    @Autowired
    private TripService tripService;

    @Test
    void createTrip() {
        // given
        TripCreateRequestDto dto = TripCreateRequestDto.builder()
                .memberId(1L)
                .tripName("부산 여행")
                .startTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .endTime(LocalDateTime.of(2025, 8, 5, 18, 0))
                .location("BUSAN")
                .memberIds(Arrays.asList(2L, 3L, 4L))
                .build();

        // when
        boolean result = tripService.createTrip(dto);

        // then
        log.info("여행 생성 결과: {}", result);
        assertTrue(result);
    }
}