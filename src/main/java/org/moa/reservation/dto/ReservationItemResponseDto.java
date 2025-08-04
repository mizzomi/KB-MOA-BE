package org.moa.reservation.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItemResponseDto {
	private String name;        // 식당명, KTX 편명, 숙소명
	private Long itemId;        // transport_id, rest_id, accom_id

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;     // 예약 날짜
	private String resKind;     // 예약 종류 (TRANSPORT, ACCOMMODATION, RESTAURANT)
}
