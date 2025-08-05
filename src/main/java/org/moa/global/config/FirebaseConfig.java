package org.moa.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import javax.annotation.PostConstruct; // jakarta -> javax 로 변경

import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @Value("${firebase.project-id}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            // 1. 환경 변수에서 키 값(JSON 문자열)을 먼저 읽어옴
            String credentialsJson = System.getenv("FIREBASE_CREDENTIALS");

            InputStream serviceAccount;

            // 2. 환경 변수가 존재하면, 문자열을 InputStream으로 변환
            if (StringUtils.hasText(credentialsJson)) {
                log.info("Firebase 초기화: 환경 변수에서 키를 로드합니다.");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            }
            // 3. 환경 변수가 없으면(로컬 개발 환경), 기존 방식대로 파일에서 읽어옴
            else {
                log.info("Firebase 초기화: 로컬 파일({})에서 키를 로드합니다.", keyPath);
                serviceAccount = new ClassPathResource(keyPath).getInputStream();
            }

            if (FirebaseApp.getApps().isEmpty()) { // 여러번 초기화되는 것 방지
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase App이 성공적으로 초기화되었습니다.");
            }
            serviceAccount.close();
        } catch (IOException e) {
            log.error("Firebase App 초기화 중 오류 발생", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance(); // 기존 Firebase 앱 사용
    }
}