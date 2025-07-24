package org.moa.member.dto.verify;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberVerifyRequestDto {
	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
	private String name;

	@NotBlank(message = "주민번호는 필수입니다.")
	@Pattern(regexp = "^\\d{13}$", message = "주민번호는 13자리 숫자여야 합니다.")
	private String idCardNumber;

	@NotBlank(message = "계좌번호는 필수입니다.")
	private String accountNumber;

	@NotBlank(message = "계좌 비밀번호는 필수입니다.")
	@Pattern(regexp = "^\\d{4}$", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
	private String accountPassword;
}
