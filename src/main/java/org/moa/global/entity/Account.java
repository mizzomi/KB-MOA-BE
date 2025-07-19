package org.moa.global.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.moa.member.type.Bank;

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
public class Account {
	private Long memberId;
	private String accountNumber;
	private String accountPassword;
	private Bank bank;
	private BigDecimal balance;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
