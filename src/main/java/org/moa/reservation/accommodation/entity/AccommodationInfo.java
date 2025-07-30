package org.moa.reservation.accommodation.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationInfo {
    private Long  accommId;
    private String hotelName;
    private String address;
    private Integer roomRemain;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;
    private String hotelImageUrl;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
