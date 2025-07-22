package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.trip.dto.settlement.ExpenseCreateRequestDto;
import org.moa.trip.entity.Expense;
import org.moa.trip.entity.Trip;
import org.moa.trip.mapper.ExpenseMapper;
import org.moa.trip.mapper.TripMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService{
    private final SettlementService settlementService;

    private final TripMapper tripMapper;
    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public boolean createExpense(ExpenseCreateRequestDto dto){
        log.info("createExpense : DTO = {}",dto);

        // 1. DTO 필수 필드 null 체크 및 금액 유효성 검사
        if (dto.getTripId() == null || dto.getMemberId() == null ||
                dto.getExpenseName() == null || dto.getExpenseName().trim().isEmpty() ||
                dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "필수 입력값이 누락되었거나 지출 금액이 유효하지 않습니다.");
        }

        // 2. 여행 정보 조회 및 존재 여부 검사
        Trip trip = tripMapper.selectTripById(dto.getTripId());
        if (trip == null) {
            throw new BusinessException(StatusCode.NOT_FOUND, "해당 ID의 여행을 찾을 수 없습니다.");
        }

        // 3. Expense 엔티티 빌드
        Expense newExpense = Expense.builder()
                .tripId(dto.getTripId())
                .memberId(dto.getMemberId()) // DTO에서 받는 결제자 ID
                .expenseName(dto.getExpenseName())
                .amount(dto.getAmount())
                .location(trip.getTripLocation()) // 한 번 조회한 trip 객체 재사용
                .settlementCompleted(false)
                .createdAt(LocalDateTime.now()) // 엔티티 생성 시각
                .updatedAt(LocalDateTime.now()) // 엔티티 업데이트 시각
                .build();

        log.info("createExpense : newExpense = {}",newExpense);

        // 4. Expense 객체 DB 저장
        try {
            expenseMapper.insert(newExpense);
        } catch (Exception e) { // DB 제약 조건 위반, DB 연결 문제 등
            log.error("비용 저장 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            // DataAccessException 등 구체적인 DB 예외를 잡아서 BusinessException으로 변환
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "비용 저장 중 서버에 문제가 발생했습니다.");
        }

        // 각 member 에 대해 공유 금액만큼 정산 생성
        for(int i=0;i<dto.getMemberIds().size();i++){
            Long memberId = dto.getMemberIds().get(i);
            BigDecimal amount = dto.getAmounts().get(i);
            settlementService.createSettlement(newExpense.getExpenseId(), newExpense.getTripId(), memberId, amount);
        }
        return true;
    }
}
