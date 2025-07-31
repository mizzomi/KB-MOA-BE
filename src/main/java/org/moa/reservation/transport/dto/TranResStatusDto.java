package org.moa.reservation.transport.dto;

import org.moa.reservation.transport.type.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranResStatusDto {
	private Long tranResId;
	private Status status;
}
