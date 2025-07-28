package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.entity.Expense;

@Mapper
public interface ExpenseMapper {
    void insert(Expense expense);
    Expense searchByExpenseId(Long expenseId);
    void updateSettlementCompleted(@Param("expenseId") Long expenseId , @Param("completed") boolean completed);
}
