package org.moa.trip.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moa.trip.entity.Trip;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateRequestDto {
    @NotBlank(message = "여행 이름은 필수입니다.")
    private String tripName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private List<Long> memberIds;
}

