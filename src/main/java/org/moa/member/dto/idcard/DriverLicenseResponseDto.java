package org.moa.member.dto.idcard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DriverLicenseResponseDto {
    private String name;
    private String licenseNumber; // 면허 번호
    private String idCardNumber; // 주민번호
    private String licenseType; // 면허 종류
    private String issuingAgency; // 발급 기관
    private String imageUrl; // 서명된 URL
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate issuedDate; // 발급일자
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate; // 만료일자
}
