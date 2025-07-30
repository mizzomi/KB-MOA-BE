package org.moa.reservation.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationInfoResponse {
    private Long accomId;
    private String hotelName;
    private String address;
    private String hotelImageUrl;
}
