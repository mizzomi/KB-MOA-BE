package org.moa.member.dto.idcard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyIdResponseDto {
    private IdCardResponseDto idCard;
    private DriverLicenseResponseDto driverLicense;
}
