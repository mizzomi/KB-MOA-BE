package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdCardMapper {
	boolean existsByNameAndIdCardNumber(@Param("name") String name, @Param("idCardNumber") String idCardNumber);

	void updateMemberIdByIdCardNumber(@Param("idCardNumber") String idCardNumber, @Param("memberId") Long memberId);
}
