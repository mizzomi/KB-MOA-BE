package org.moa.trip.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moa.trip.entity.Trip;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateRequestDto {
    private Long memberId;
    private String tripName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private List<Long> memberIds;
}
