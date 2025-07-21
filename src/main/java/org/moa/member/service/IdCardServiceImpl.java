package org.moa.member.service;

import org.moa.member.mapper.IdCardMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdCardServiceImpl implements IdCardService {

	private final IdCardMapper idCardMapper;

	@Override
	public boolean validateNameAndIdCardNumber(String name, String idCardNumber) {
		return idCardMapper.existsByNameAndIdCardNumber(name, idCardNumber);
	}
}
