package org.moa.member.service;

import org.moa.global.mapper.AccountMapper;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.entity.Member;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.moa.member.mapper.MemberMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final MemberMapper memberMapper;
	private final IdCardMapper idCardMapper;
	private final AccountMapper accountMapper;
	private final DriverLicenseMapper driverLicenseMapper;

	// @Override
	// public boolean checkDuplicate(String email) {
	// 	Member member = memberMapper.get(email);
	// 	return member != null;
	// }
	//
	// @Override
	// public Member get(String email) {
	// 	Member member = Optional.ofNullable(memberMapper.get(email))
	// 		.orElseThrow(() -> new NoSuchElementException("해당 이메일의 사용자가 없습니다."));
	// 	return member;
	// }

	@Transactional
	@Override
	public boolean userJoin(MemberJoinRequestDto dto) {

		//이름과 주민등록번호에 맞는 사람이 있는지 확인
		if (!validateNameAndIdCardNumber(dto.getName(), dto.getIdCardNumber())) {
			throw new IllegalArgumentException("일치하는 신분증 정보가 없습니다.");
		}

		//계좌번호와 계좌비밀번호에 맞는 사람이 있는지 확인
		if (!validateAccountNumber(dto.getAccountNumber(), dto.getAccountPassword())) {
			throw new IllegalArgumentException("일치하는 계좌 정보가 없습니다.");
		}

		//새로운 멤버 생성후 DB에 저장
		Member member = MemberJoinRequestDto.toVO(dto);
		memberMapper.insert(member);
		Long memberId = member.getMemberId();

		idCardMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		driverLicenseMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		accountMapper.updateMemberIdByAccountNumber(dto.getAccountNumber(), memberId);

		return true;

	}

	public boolean validateAccountNumber(String accountNumber, String accountPassword) {
		return accountMapper.existsByAccountNumberAndAccountPassword(accountNumber, accountPassword);
	}

	public boolean validateNameAndIdCardNumber(String name, String idCardNumber) {
		return idCardMapper.existsByNameAndIdCardNumber(name, idCardNumber);
	}
}
