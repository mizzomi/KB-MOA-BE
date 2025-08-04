package org.moa.reservation.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantReservationInsertDto {
    private Long reservationId;
    private Long tripId;
    private Long tripDayId;
    private Long restId;
    private Long restTimeId;
    private String resTime;
    private Integer resNum;
}
