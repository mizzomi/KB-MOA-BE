package org.moa.reservation.dto.qr;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccommodationReservationDto {
    private String type;
    private Long reservationId;
    private String hotelName;
    private String address;
    private String roomType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime checkinDay;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime checkoutDay;
    private Integer guests;
    private String location;
    private String status;
}