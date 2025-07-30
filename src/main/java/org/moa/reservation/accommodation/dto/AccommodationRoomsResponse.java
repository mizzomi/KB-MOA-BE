package org.moa.reservation.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationRoomsResponse {
    private String accomResId;
    private Integer maxGuests;
    private String hotelName;
    private BigDecimal price;
    private String roomType;
    private String roomImageUrl;
}
