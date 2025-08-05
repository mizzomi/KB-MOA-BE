package org.moa.member.dto.idcard;

import lombok.Data;

import java.time.LocalDate;

@Data
public class IdCardRawData {
    private String name;
    private String address;
    private String imageUrl;
    private String idCardNumber;
    private LocalDate issuedDate; // 발급일자

}
