package org.moa.reservation.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantReservationResponseDto {
    private Long reservationId;
    private Long restId;
    private String restName;
    private String date;
    private String time;
    private Integer resNum;
    private String status;
}
