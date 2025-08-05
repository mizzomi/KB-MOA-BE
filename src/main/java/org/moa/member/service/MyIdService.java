package org.moa.member.service;

import org.moa.member.dto.idcard.MyIdResponseDto;

public interface MyIdService {
    MyIdResponseDto getMyIdInfo(Long memberId);
}
