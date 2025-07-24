package org.moa.member.service;

import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.dto.verify.MemberVerifyRequestDto;
import org.moa.member.entity.Member;

public interface MemberService {
	// boolean checkDuplicate(String email);
	//
	// Member get(String email);

	Member getByMemberId(Long memberId);

	boolean userJoin(MemberJoinRequestDto dto);

	boolean verifyJoin(MemberVerifyRequestDto dto);

	Long searchUserIdByEmail(String email);
}
