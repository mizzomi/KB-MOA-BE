package org.moa.trip.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moa.trip.type.Location;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripLocationResponseDto {
    private Location locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
}
