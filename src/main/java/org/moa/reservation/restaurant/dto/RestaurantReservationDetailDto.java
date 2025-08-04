package org.moa.reservation.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantReservationDetailDto {
    private Long restResId;
    private Long reservationId;
    private Long tripDayId;
    private Long restId;
    private String restName;
    private String address;
    private String category;
    private String imageUrl;
    private String date;
    private String time;
    private Integer resNum;
    private String status;
}