package org.moa.reservation.restaurant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantReservationRequestDto {
    private Long tripId;
    private Long restId;
    private String date;
    private String time;
    private Integer resNum;
}
