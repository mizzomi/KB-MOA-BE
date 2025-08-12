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
public class UserTransportReservationDto {
    private Long tranResId; // 좌석 고유 ID
    private String type;
    private Long reservationId;
    private Long transportId;
    private String trainNo;
    private String departureName;
    private String arrivalName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime departureTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime arrivalTime;
    private Integer seatRoomNo;
    private String seatNumber;
    private String seatType;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bookedAt;
}