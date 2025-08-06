package org.moa.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
	private Long reservationId; // 예약 ID
	private String name;        // 식당명, KTX 편명, 숙소명
	private Long itemId;        // transport_id, rest_id, accom_id
	private String imageUrl;    // 이미지 URL

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;     // 예약 날짜
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;  // 예매시간
	
	private String resKind;     // 예약 종류 (TRANSPORT, ACCOMMODATION, RESTAURANT)
}