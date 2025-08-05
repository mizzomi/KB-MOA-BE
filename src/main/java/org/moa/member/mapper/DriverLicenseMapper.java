package org.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.member.dto.idcard.DriverLicenseRawData;
import org.moa.member.dto.idcard.DriverLicenseResponseDto;

@Mapper
public interface DriverLicenseMapper {
	void updateMemberIdByIdCardNumber(@Param("idCardNumber") String idCardNumber, @Param("memberId") Long memberId);

	DriverLicenseRawData findDriverLicenseByMemberId(@Param("memberId") Long memberId);
}
