package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.mapper.SettlementMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements  SettlementService {
    private final SettlementMapper settlementMapper;

    public boolean createSettlement(Long expenseId, Long tripId, Long creatorId, Long memberId, BigDecimal amount) {
        log.info("createSettlement");
        SettlementNotes settlementNotes = SettlementNotes.builder()
                .expenseId(expenseId)
                .tripId(tripId)
                .memberId(memberId)
                .shareAmount(amount)
                // 정산 생성자 ID = 리스트에 담긴 ID -> 자동 정산 완료 처리
                // 정산 생성자 ID != 리스트에 담긴 ID -> 정산 해야함
                .isPayed(creatorId.equals(memberId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try{
            settlementMapper.insert(settlementNotes);
            // 이때 여기서 사용자에게 정산 요청 알림을 생성
        } catch  (Exception e) {
            log.error("정산 내역 저장 중 데이터베이스 오류 발생: {}", e.getMessage());
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 저장 중 서버에 문제가 발생했습니다.");
        }
        return true;
    }
}
