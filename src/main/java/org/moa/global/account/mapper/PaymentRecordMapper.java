package org.moa.global.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.global.account.entity.PaymentRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentRecordMapper {
	void insert(PaymentRecord paymentRecord);

	void updateTripDayId(@Param("recordId") Long recordId, @Param("tripDayId") Long tripDayId);

	List<PaymentRecord> searchByPaymentDates(@Param("paymentDates") List<LocalDateTime> paymentDates, @Param("memberId") Long memberId);
	List<PaymentRecord> searchByIdsAndMemberId(
			@Param("recordIds") List<Long> recordIds);
	List<PaymentRecord> searchByTripDayIds(@Param("tripDayIds") List<Long> tripDayIds);
}
