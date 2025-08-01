package org.moa.reservation.transport.dto;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransResRequestDto {
	@NotNull(message="여행 ID는 필수입니다.")
	Long tripId;
	@NotEmpty(message="예약슬롯 ID목록은 비어있을 수 없습니다.")
	List<Long> tranResIds;
	@NotNull(message="예약일은 필수입니다.")
	LocalDateTime departureDateTime;
}
