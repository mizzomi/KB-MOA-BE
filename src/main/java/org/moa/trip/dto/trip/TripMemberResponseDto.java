package org.moa.trip.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMemberResponseDto {
    private Long memberId;
    private String memberName;
    private String memberEmail;
}
