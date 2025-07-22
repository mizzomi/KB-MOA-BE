package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.SettlementNotes;
import org.moa.trip.entity.Trip;

@Mapper
public interface SettlementMapper {
    public void insert(SettlementNotes settlementNotes);

}
