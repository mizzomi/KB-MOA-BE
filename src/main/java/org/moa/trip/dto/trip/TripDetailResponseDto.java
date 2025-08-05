package org.moa.trip.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailResponseDto {
    private Long tripId;
    private String tripName;
    private String startDate;
    private String endDate;
    private String locationName;
    private String status;
}
