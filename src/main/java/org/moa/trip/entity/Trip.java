package org.moa.trip.entity;

import lombok.*;
import org.moa.trip.type.Location;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    private Long tripId;
    private Long memberId;
    private String tripName;
    private Location tripLocation;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 여행 멤버들
    private List<TripMember> tripMembers;
    // 여행 기록들
    private List<TripRecord> tripRecords;
    // 여행 비용들
    private List<Expense> expenses;
    // 여행 날짜들
    private List<TripDay> tripDays;
}
