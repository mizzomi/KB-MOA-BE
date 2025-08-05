package org.moa.member.dto.idcard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class IdCardResponseDto {
    private String name;
    private String idCardNumber; // 주민번호
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthday; // 계산된 생년월일
    private String address;
    private String imageUrl; // 서명된 URL
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate issuedDate; // 발급일자

}
