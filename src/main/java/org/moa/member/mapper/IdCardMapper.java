package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.member.dto.idcard.IdCardRawData;
import org.moa.member.entity.IdCard;

@Mapper
public interface IdCardMapper {
	boolean existsByNameAndIdCardNumber(@Param("name") String name, @Param("idCardNumber") String idCardNumber);

	void updateMemberIdByIdCardNumber(@Param("idCardNumber") String idCardNumber, @Param("memberId") Long memberId);

	// QR 생성을 위해 memberId로 주민등록증 정보 조회
	IdCard findByMemberId(@Param("memberId") Long memberId);

	IdCardRawData findIdCardByMemberId(@Param("memberId") Long memberId);
}
