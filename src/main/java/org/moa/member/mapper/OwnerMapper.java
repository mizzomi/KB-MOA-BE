package org.moa.member.mapper;

import org.apache.ibatis.annotations.Param;

public interface OwnerMapper {
    boolean isOwnerOfBusiness(@Param("ownerId") Long ownerId,
                              @Param("businessType") String businessType,
                              @Param("businessId") Long businessId);
}
