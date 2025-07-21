package org.moa.member.entity;

import java.time.LocalDateTime;

import org.moa.member.type.MemberRole;

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
public class Member {
	private Long memberId;
	private MemberRole memberType;
	private String email;
	private String password;
	private String name;
	private String idCardNumber;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
