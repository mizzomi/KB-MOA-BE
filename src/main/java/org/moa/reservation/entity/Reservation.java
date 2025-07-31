package org.moa.reservation.entity;

import org.moa.global.type.ResKind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
	Long reservationId;
	Long tripDayId;
	ResKind resKind;
}
