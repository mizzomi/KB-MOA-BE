package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.member.entity.Member;

@Mapper
public interface MemberMapper {

	int insert(Member member);

    Member getByMemberId(Long memberId);

    Member getByEmail(String email);
}
