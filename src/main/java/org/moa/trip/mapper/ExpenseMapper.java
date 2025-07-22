package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.Expense;

@Mapper
public interface ExpenseMapper {
    public void insert(Expense expense);
}
