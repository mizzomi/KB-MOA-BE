package org.moa.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import javax.annotation.PostConstruct;

import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

    private boolean firebaseEnabled = false;

    @PostConstruct
    public void initialize() {
        try {
            // 이미 초기화되어 있는지 먼저 확인
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase App이 이미 초기화되어 있습니다.");
                firebaseEnabled = true;
                return;
            }
            
            // 1. 환경 변수에서 키 값(JSON 문자열)을 먼저 읽어옴
            String credentialsJson = System.getenv("FIREBASE_CREDENTIALS");

            InputStream serviceAccount = null;

            // 2. 환경 변수가 존재하면, 문자열을 InputStream으로 변환
            if (StringUtils.hasText(credentialsJson)) {
                log.info("Firebase 초기화: 환경 변수에서 키를 로드합니다.");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            }
            // 3. Docker 환경에서 절대 경로로 시도
            else if (keyPath != null && keyPath.startsWith("/")) {
                File file = new File(keyPath);
                if (file.exists() && file.canRead()) {
                    log.info("Firebase 초기화: 절대 경로 파일({})에서 키를 로드합니다.", keyPath);
                    serviceAccount = new FileInputStream(file);
                } else {
                    log.warn("Firebase 키 파일을 찾을 수 없습니다: {}", keyPath);
                }
            }
            // 4. 클래스패스에서 시도 (로컬 개발 환경)
            else if (keyPath != null) {
                try {
                    Resource resource = new FileSystemResource("src/main/resources/" + keyPath);
                    if (resource.exists()) {
                        log.info("Firebase 초기화: 클래스패스 파일({})에서 키를 로드합니다.", keyPath);
                        serviceAccount = resource.getInputStream();
                    }
                } catch (Exception e) {
                    log.warn("Firebase 키 파일을 클래스패스에서 찾을 수 없습니다: {}", keyPath);
                }
            }

            // Firebase 초기화
            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
                firebaseEnabled = true;
                log.info("Firebase App이 성공적으로 초기화되었습니다.");
                serviceAccount.close();
            } else {
                log.warn("Firebase 키 파일을 찾을 수 없어 초기화하지 못했습니다. FCM 기능이 비활성화됩니다.");
                firebaseEnabled = false;
            }
        } catch (Exception e) {
            log.error("Firebase App 초기화 중 오류 발생 - FCM 기능이 비활성화됩니다.", e);
            firebaseEnabled = false;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (firebaseEnabled) {
            try {
                FirebaseMessaging messaging = FirebaseMessaging.getInstance();
                log.info("FirebaseMessaging Bean이 성공적으로 생성되었습니다.");
                return messaging;
            } catch (Exception e) {
                log.error("FirebaseMessaging Bean 생성 실패", e);
            }
        }
        log.warn("Firebase가 비활성화되어 있습니다. Null FirebaseMessaging을 반환합니다.");
        return null;  // FCM이 없어도 애플리케이션은 실행되도록
    }
}