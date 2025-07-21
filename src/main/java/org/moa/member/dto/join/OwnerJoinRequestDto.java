package org.moa.member.dto.join;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerJoinRequestDto {
	private String ownerPassword;
	private String ownerNo;
}
