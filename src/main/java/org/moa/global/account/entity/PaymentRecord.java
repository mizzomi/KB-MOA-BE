package org.moa.global.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {
	private Long recordId;
	private Long accountId;
	private Long memberId;

	private String paymentName;
	private BigDecimal paymentPrice;
	private LocalDateTime paymentDate;
	private String paymentLocation;
}
