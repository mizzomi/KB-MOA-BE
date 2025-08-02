package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.entity.Trip;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper
public interface SettlementMapper {
    void insert(SettlementNotes settlementNotes);
    void updateIsPayedByExpenseIdAndMemberID(@Param("expenseId") Long expenseId, @Param("memberId") Long memberId);
    List<SettlementNotes> searchByMemberIdAndTripId(@Param("memberId") Long memberId,@Param("tripId") Long tripId, @Param("pageable") Pageable pageable);
    List<SettlementNotes> searchByExpenseId(Long expenseId);
    SettlementNotes searchByMemberIdAndExpenseId(@Param("expenseId") Long expenseId, @Param("memberId") Long memberId);
}
