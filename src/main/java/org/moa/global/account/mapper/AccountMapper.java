package org.moa.global.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.global.account.entity.Account;
import org.springframework.security.core.parameters.P;

import java.math.BigDecimal;

@Mapper
public interface AccountMapper {
	boolean existsByAccountNumberAndAccountPassword(@Param("accountNumber") String accountNumber,
		@Param("accountPassword") String accountPassword);

	boolean existsByNameAndAccountNumberAndAccountPassword(@Param("name") String name,
		@Param("accountNumber") String accountNumber, @Param("accountPassword") String accountPassword);

	void transactionBalance(@Param("receiverId")Long receiverId, @Param("senderId") Long senderId, @Param("amount") BigDecimal amount);

	void updateMemberIdByAccountNumber(@Param("accountNumber") String accountNumber, @Param("memberId") Long memberId);

	Account searchAccountByMemberId(@Param("memberId") Long memberId);
	Account searchAccountByMemberIdForUpdate(@Param("memberId") Long memberId);
}
