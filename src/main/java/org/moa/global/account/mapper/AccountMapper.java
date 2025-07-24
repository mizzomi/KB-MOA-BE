package org.moa.global.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
	boolean existsByAccountNumberAndAccountPassword(@Param("accountNumber") String accountNumber,
		@Param("accountPassword") String accountPassword);

	boolean existsByNameAndAccountNumberAndAccountPassword(@Param("name") String name,
		@Param("accountNumber") String accountNumber, @Param("accountPassword") String accountPassword);

	void updateMemberIdByAccountNumber(@Param("accountNumber") String accountNumber, @Param("memberId") Long memberId);
}
