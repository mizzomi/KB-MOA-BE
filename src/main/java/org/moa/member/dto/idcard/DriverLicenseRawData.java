package org.moa.member.dto.idcard;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverLicenseRawData {
    private String name;
    private String issuingAgency;
    private String imageUrl; // DB에 저장된 파일 이름
    private String idCardNumber; // 생년월일 계산용
}