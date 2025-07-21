package org.moa.member.service;

import org.moa.member.dto.join.MemberJoinRequestDto;

public interface MemberService {
	// boolean checkDuplicate(String email);
	//
	// Member get(String email);

	boolean userJoin(MemberJoinRequestDto dto);

}
