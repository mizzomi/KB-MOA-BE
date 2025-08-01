package org.moa.reservation.transport.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransPaymentRequestDto {
	@NotNull(message="예약 ID는 필수입니다.")
	private Long reservationId;

	@NotNull(message="결제 금액은 필수입니다.")
	private BigDecimal price;
}
