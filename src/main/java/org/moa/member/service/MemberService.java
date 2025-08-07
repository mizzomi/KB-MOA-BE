package org.moa.member.service;

import org.moa.global.notification.dto.FcmTokenRequestDto;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.dto.verify.MemberVerifyRequestDto;
import org.moa.member.entity.Member;
import org.moa.trip.dto.trip.TripMemberResponseDto;

import java.util.List;

public interface MemberService {
	// boolean checkDuplicate(String email);
	//
	// Member get(String email);
	boolean updateFcmToken(FcmTokenRequestDto dto);

	boolean deleteFcmToken();

	Member getByMemberId(Long memberId);

	boolean userJoin(MemberJoinRequestDto dto);

	boolean verifyJoin(MemberVerifyRequestDto dto);

	Long searchUserIdByEmail(String email, Long tripId);

	List<TripMemberResponseDto> getTripMembers(Long tripId);
}
