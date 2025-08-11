package org.moa.global.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.moa.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmService {
    private final FirebaseMessaging firebaseMessaging;
    private final MemberMapper memberMapper;

    @Autowired
    public FcmService(@Autowired(required = false) FirebaseMessaging firebaseMessaging, 
                      MemberMapper memberMapper) {
        this.firebaseMessaging = firebaseMessaging;
        this.memberMapper = memberMapper;
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging이 초기화되지 않았습니다. FCM 기능이 비활성화됩니다.");
        }
    }

    public void sendNotification(Long memberId, String title, String content){
        if (firebaseMessaging == null) {
            log.warn("FCM이 비활성화되어 있어 알림을 전송할 수 없습니다.");
            return;
        }
        
        try{
            String fcmToken = memberMapper.searchTokenById(memberId);
            if(fcmToken == null){
                log.info("fcmToken is null");
                return;
            }
            // 메시지 생성
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(content)
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("fcmToken 전송 응답 : {}", response);
        }
        catch (Exception e){
            log.info("fcmToken 전송 에러 : {}", e.getMessage());
        }
    }
}