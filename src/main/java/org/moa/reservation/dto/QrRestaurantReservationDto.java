package org.moa.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrRestaurantReservationDto {
    private String type; // 식당 예약
    private Long reservationId;
    private Long restId;
    private String restName;
    private String date;
    private String time;
    private Integer resNum;
    private String status;
}