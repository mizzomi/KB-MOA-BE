package org.moa.member.dto.idcard;

import lombok.Data;

@Data
public class IdCardRawData {
    private String name;
    private String address;
    private String imageUrl;
    private String idCardNumber; // 생년월일 계산용

}
