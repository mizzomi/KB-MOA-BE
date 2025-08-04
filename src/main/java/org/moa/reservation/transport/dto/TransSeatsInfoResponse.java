package org.moa.reservation.transport.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransSeatsInfoResponse {
	private Long tranResId;
	private Integer seatRoomNo;
	private String seatNumber;
	private String seatType;
	private String status;
	private BigDecimal price;
}
