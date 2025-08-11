package org.moa.reservation.transport.helper;

import org.moa.reservation.transport.dto.TransSeatsInfoResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 좌석 그룹화 관련 헬퍼 클래스
// 좌석 정보를 룸 번호별로 그룹화하는 로직을 분리
@Component
public class SeatGroupingHelper {
    
    // 좌석 목록을 룸 번호별로 그룹화
    // @param seats 좌석 목록
    // @return 룸 번호별로 그룹화된 좌석 정보 (순서 유지)
    public Map<Integer, List<TransSeatsInfoResponse>> groupSeatsByRoom(List<TransSeatsInfoResponse> seats) {
        return seats.stream()
            .collect(Collectors.groupingBy(
                TransSeatsInfoResponse::getSeatRoomNo,
                LinkedHashMap::new,       // insertion-order 유지
                Collectors.toList()
            ));
    }
}
