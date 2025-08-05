package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.member.entity.Member;

import java.util.List;

@Mapper
public interface MemberMapper {

	int insert(Member member);

    String searchTokenById(Long memberId);

    Member getByMemberId(Long memberId);

    Member getByEmail(String email);
}
