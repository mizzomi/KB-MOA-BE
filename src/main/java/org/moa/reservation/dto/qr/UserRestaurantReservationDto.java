package org.moa.reservation.dto.qr;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRestaurantReservationDto {
    private String type;
    private Long reservationId;
    private String restName;
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;
    private Integer resNum;
    private String status;
}