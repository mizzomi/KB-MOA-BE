package org.moa.global.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.member.mapper.MemberMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {
    private final FirebaseMessaging firebaseMessaging;
    private final MemberMapper memberMapper;

    public void sendNotification(Long memberId, String title, String content){
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
