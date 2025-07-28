package org.moa.global.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.global.account.entity.PaymentRecord;

@Mapper
public interface PaymentRecordMapper {
	void insert(PaymentRecord paymentRecord);
}
