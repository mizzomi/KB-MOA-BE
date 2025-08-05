package org.moa.global.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.notification.dto.NotificationResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    @Override
    @Transactional
    public List<NotificationResponseDto> getNotifications(){
        // member_id = memberId AND is_read = false 인 데이터 찾기~
        return null;
    };

    @Override
    @Transactional
    public boolean tripNotification(){
        return false;
    }
    @Override
    @Transactional
    public boolean settleNotification(){
        return false;
    }
}
