package org.moa.trip.entity;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TripRecordsImages {
    private Long imageId;
    private Long recordId;
    private String imageUrl;
}
