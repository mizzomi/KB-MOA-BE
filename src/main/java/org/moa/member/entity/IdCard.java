package org.moa.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class IdCard {
	private Long memberId;
	private String idCardNumber;
	private String name;
	private LocalDate issuedDate;
	private String address;
	private String imageUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
