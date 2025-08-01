package org.moa.reservation.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranstInfoResponse {
	private Long transportId;
	private String departureName;
	private String trainNo;
}
