package org.moa.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {
    
    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}
