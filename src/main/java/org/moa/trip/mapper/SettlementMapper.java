package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.entity.Trip;

import java.util.List;

@Mapper
public interface SettlementMapper {
    public void insert(SettlementNotes settlementNotes);
    public List<SettlementNotes> searchByMemberIdAndTripId(Long memberId, Long tripId);
    public List<SettlementNotes> searchByExpenseId(Long expenseId);
    SettlementNotes searchByMemberIdAndExpenseId(@Param("expenseId") Long expenseId, @Param("memberId") Long memberId);
}
