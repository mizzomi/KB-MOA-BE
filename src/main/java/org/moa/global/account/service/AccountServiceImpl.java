package org.moa.global.account.service;

import org.moa.global.account.mapper.AccountMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private final AccountMapper accountMapper;

	@Override
	public boolean validateAccountNumber(String accountNumber, String accountPassword) {
		return accountMapper.existsByAccountNumberAndAccountPassword(accountNumber, accountPassword);
	}
}
