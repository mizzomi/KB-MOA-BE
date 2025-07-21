package org.moa.trip.entity;

import lombok.*;
import org.moa.trip.type.TripRole;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripMember {
    private Long tripMemberId;
    private Long tripId;
    private Long memberId;
    private TripRole role;
    private LocalDateTime joinedAt;
}
