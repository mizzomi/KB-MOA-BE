package org.moa.member.service;

import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.util.HashUtil;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.entity.Member;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.moa.member.mapper.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	private final MemberMapper memberMapper;
	private final IdCardMapper idCardMapper;
	private final AccountMapper accountMapper;
	private final DriverLicenseMapper driverLicenseMapper;
	private final HashUtil hashUtil;

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
	public Member getByMemberId(Long memberId) {
		return memberMapper.getByMemberId(memberId);
	}

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

		// Member 생성 및 저장
		Member member = Member.builder()
			.memberType(dto.getRole())
			.email(dto.getEmail())
			.password(hashUtil.hash(dto.getPassword()))
			.name(dto.getName())
			.idCardNumber(hashUtil.hash(dto.getIdCardNumber()))
			.createdAt(java.time.LocalDateTime.now())
			.updatedAt(java.time.LocalDateTime.now())
			.build();

		//새로운 멤버 생성후 DB에 저장
		memberMapper.insert(member);
		Long memberId = member.getMemberId();

		//주민등록증 - 멤버 매핑
		idCardMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		//운전면허증 - 멤버 매핑
		driverLicenseMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		//계좌 - 멤버 매핑
		accountMapper.updateMemberIdByAccountNumber(dto.getAccountNumber(), memberId);

		return true;

	}

	public boolean validateAccountNumber(String accountNumber, String accountPassword) {
		return accountMapper.existsByAccountNumberAndAccountPassword(accountNumber, accountPassword);
	}

	public boolean validateNameAndIdCardNumber(String name, String idCardNumber) {
		return idCardMapper.existsByNameAndIdCardNumber(name, idCardNumber);
	}

	@Override
	public Long searchUserIdByEmail(String email){
		return memberMapper.getByEmail(email).getMemberId();
	}
}
