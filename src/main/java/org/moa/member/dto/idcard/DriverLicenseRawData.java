package org.moa.member.dto.idcard;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverLicenseRawData {
    private String name;
    private String licenseNumber;
    private String idCardNumber;
    private String licenseType;
    private String issuingAgency;
    private String imageUrl; // DB에 저장된 파일 이름
    private LocalDate issuedDate; // 발급일자
    private LocalDate expiryDate; // 만료일자
}