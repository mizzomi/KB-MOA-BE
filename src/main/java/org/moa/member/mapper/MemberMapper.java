package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.member.entity.Member;

@Mapper
public interface MemberMapper {

	int insert(Member member);

	String selectUserIdByEmail(@Param("email") String email);
}
