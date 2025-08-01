package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.dto.expense.ExpenseCreateRequestDto;
import org.moa.trip.dto.expense.ExpenseResponseDto;
import org.moa.trip.entity.Expense;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.entity.Trip;
import org.moa.trip.mapper.ExpenseMapper;
import org.moa.trip.mapper.SettlementMapper;
import org.moa.trip.mapper.TripMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService{
    private final SettlementService settlementService;

    private final TripMapper tripMapper;
    private final ExpenseMapper expenseMapper;
    private final SettlementMapper settlementMapper;
    private final MemberMapper memberMapper;

    @Override
    @Transactional
    public boolean createExpense(ExpenseCreateRequestDto dto){
        log.info("createExpense : DTO = {}",dto);

        // 1. DTO 필수 필드 null 체크 및 금액 유효성 검사
        if (dto.getTripId() == null ||
                dto.getExpenseName() == null || dto.getExpenseName().trim().isEmpty() ||
                dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "필수 입력값이 누락되었거나 지출 금액이 유효하지 않습니다.");
        }

        // 2. 여행 정보 조회 및 존재 여부 검사
        Trip trip = tripMapper.searchTripById(dto.getTripId());
        if (trip == null) {
            throw new BusinessException(StatusCode.NOT_FOUND, "해당 ID의 여행을 찾을 수 없습니다.");
        }
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberMapper.getByEmail(userDetails.getUsername());
        // 3. Expense 엔티티 빌드
        Expense newExpense = Expense.builder()
                .tripId(dto.getTripId())
                .memberId(member.getMemberId()) // DTO에서 받는 결제자 ID
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
        for(int i=0;i<dto.getExpenses().size();i++){
            Long creatorId = member.getMemberId();
            Long memberId = dto.getExpenses().get(i).getMemberId();
            BigDecimal amount = dto.getExpenses().get(i).getAmount();
            settlementService.createSettlement(newExpense.getExpenseId(), newExpense.getTripId(), creatorId , memberId, amount);
        }
        return true;
    }

    @Override
    @Transactional
    public Page<ExpenseResponseDto> getExpenses(Long tripId, Pageable pageable){
        log.info("getExpenses 호출: tripId={}",tripId);
        if (tripId == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "회원 ID 또는 여행 ID가 누락되었습니다.");
        }

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = memberMapper.getByEmail(userDetails.getUsername()).getMemberId();

        // 해당 여행의 해당 유저의 정산 내역을 불러옴
        List<SettlementNotes> settlementNotes;
        try {
            settlementNotes = settlementMapper.searchByMemberIdAndTripId(memberId, tripId, pageable);
        } catch (DataAccessException e) {
            throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 내역 조회 중 서버 오류가 발생했습니다.");
        }

        if (settlementNotes.isEmpty()) {
            log.info("getExpenses: 사용자 {}의 여행 {}에 대한 정산 내역이 없습니다.", memberId, tripId);
            return new PageImpl<>(Collections.emptyList(),pageable,0);
        }
        int total = settlementNotes.size();

        List<ExpenseResponseDto> responseDtos = new ArrayList<>();
        // 각 정산 내역마다 처리
        for(SettlementNotes s : settlementNotes){
            Long expenseId = s.getExpenseId();

            Expense expense;
            try {
                expense = expenseMapper.searchByExpenseId(expenseId);
            } catch (DataAccessException e) {
                log.error("getExpenses: expenseMapper.searchByExpenseId DB 오류 발생 (expenseId={}) - {}", expenseId, e.getMessage(), e);
                throw new BusinessException(StatusCode.INTERNAL_ERROR, "비용 정보 조회 중 서버 오류가 발생했습니다.");
            }

            if (expense == null) {
                log.error("getExpenses: expenseId {}에 해당하는 Expense 를 찾을 수 없습니다. 데이터 무결성 오류.", expenseId);
                throw new BusinessException(StatusCode.INTERNAL_ERROR, "정산 처리 중 내부 데이터 오류가 발생했습니다. (관련 비용을 찾을 수 없음)");
            }

            // DTO 에 넣을 것들
            LocalDateTime expenseDate = expense.getExpenseDate();
            BigDecimal shareAmount = s.getShareAmount();
            Boolean received = s.getReceived();
            String status = getString(s, expense, received);
            ExpenseResponseDto dto = ExpenseResponseDto.builder()
                    .expenseId(expenseId)
                    .expenseDate(expenseDate)
                    .shareAmount(!received ? expense.getAmount() : shareAmount)
                    .received(received)
                    .status(status)
                    .build();
            responseDtos.add(dto);
            log.info("received : {}, shareAmount : {}, status : {}",dto.getReceived(), dto.getShareAmount(),dto.getStatus());
        }
        return new PageImpl<>(responseDtos, pageable, total);
    }

    public static String getString(SettlementNotes s, Expense expense, Boolean received) {
        Boolean isPayed = s.getIsPayed();
        Boolean settlementCompleted = expense.getSettlementCompleted();
        String status;
        if(!received){ // 보낸 요청일 때
            if(settlementCompleted){ // 정산이 완료되었을 때
                status = "정산 완료";
            }
            else{ // 정산이 완료되지 않았을 때
                status = "정산 진행중";
            }
        } else {
            if(isPayed){
                if(settlementCompleted){ // 정산이 완료되었을 때
                    status = "정산 완료";
                }
                else{ // 정산이 완료되지 않았을 때
                    status = "정산 진행중";
                }
            }
            else{
                status = "정산 하기";
            }
        }
        return status;
    }
}
