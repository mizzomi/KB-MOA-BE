package org.moa.member.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdCardResponseDto {
    private String name;
    private String idCardNumber;
    private String issuedDate;
    private String address;
    private String imageUrl;
}
