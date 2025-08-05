package org.moa.member.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.member.dto.idcard.MyIdResponseDto;
import org.moa.member.service.MyIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-id")
@RequiredArgsConstructor
public class MyIdController {
    private final MyIdService myIdService;

    @GetMapping
    public ResponseEntity<ApiResponse<MyIdResponseDto>> getMyIdInfo(
            @AuthenticationPrincipal CustomUser loginUser) {

        Long memberId = loginUser.getMember().getMemberId();
        MyIdResponseDto myIdInfo = myIdService.getMyIdInfo(memberId);

        return ResponseEntity.ok(ApiResponse.of(myIdInfo));
    }
}
