# Spring Legacy 프로젝트 실행 가이드

## 사전 준비사항

### 1. MySQL 실행
```bash
# MySQL 서비스 시작
brew services start mysql  # Mac
sudo systemctl start mysql  # Linux

# 데이터베이스 접속
mysql -u root -p

# 테이블 생성 (Refresh Token용)
CREATE TABLE IF NOT EXISTS tbl_refresh_token (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_family VARCHAR(255) NOT NULL,
    expiry_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    
    INDEX idx_member_id (member_id),
    INDEX idx_token (token),
    INDEX idx_token_family (token_family),
    
    CONSTRAINT fk_refresh_token_member 
        FOREIGN KEY (member_id) 
        REFERENCES tbl_member(member_id) 
        ON DELETE CASCADE
);
```

### 2. Redis 실행
```bash
# Redis 서비스 시작
brew services start redis  # Mac
redis-server              # 또는 직접 실행

# Redis 연결 확인
redis-cli ping
# PONG이 출력되면 정상
```

## 실행 방법

### 방법 1: IntelliJ IDEA에서 실행

1. **Tomcat 설정**
   - Run → Edit Configurations
   - + 버튼 → Tomcat Server → Local
   - Application Server: Tomcat 설치 경로 지정
   - Deployment 탭 → + → Artifact → MOA:war exploded 선택
   - Application context: `/` 로 설정

2. **실행**
   - Run 버튼 클릭
   - 브라우저에서 http://localhost:8080 접속

### 방법 2: 명령줄에서 WAR 파일 실행

1. **WAR 파일 빌드**
```bash
./gradlew clean war
```

2. **WAR 파일 위치 확인**
```bash
ls -la build/libs/
# MOA-1.0-SNAPSHOT.war 파일 확인
```

3. **Tomcat에 배포**
```bash
# Tomcat webapps 폴더에 복사
cp build/libs/MOA-1.0-SNAPSHOT.war $TOMCAT_HOME/webapps/ROOT.war

# Tomcat 시작
$TOMCAT_HOME/bin/startup.sh
```

### 방법 3: Gradle Tomcat 플러그인 사용 (선택사항)

build.gradle에 추가:
```gradle
plugins {
    id 'com.bmuschko.tomcat' version '2.7.0'
}

dependencies {
    def tomcatVersion = '9.0.58'
    tomcat "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}"
    tomcat "org.apache.tomcat.embed:tomcat-embed-logging-juli:9.0.0.M6"
    tomcat "org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}"
}

tomcat {
    httpProtocol = 'org.apache.coyote.http11.Http11Nio2Protocol'
    ajpProtocol  = 'org.apache.coyote.ajp.AjpNio2Protocol'
    httpPort = 8080
    contextPath = '/'
}
```

실행:
```bash
./gradlew tomcatRun
```

## API 테스트

### 1. 로그인
```bash
curl -X POST http://localhost:8080/api/public/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 2. API 호출 (Access Token 사용)
```bash
curl -X GET http://localhost:8080/api/some-endpoint \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

### 3. 토큰 갱신
```bash
curl -X POST http://localhost:8080/api/public/auth/refresh \
  -H "Cookie: refreshToken={REFRESH_TOKEN}"
```

### 4. 로그아웃
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Cookie: refreshToken={REFRESH_TOKEN}"
```

## 트러블슈팅

### 1. Redis 연결 실패
- Redis 서비스 실행 확인: `redis-cli ping`
- application.properties의 redis.host, redis.port 확인

### 2. MySQL 연결 실패
- MySQL 서비스 실행 확인
- 데이터베이스 생성 확인: `CREATE DATABASE IF NOT EXISTS moa;`
- application.properties의 jdbc.url, username, password 확인

### 3. 포트 충돌
- 8080 포트 사용 중인 프로세스 확인:
  ```bash
  lsof -i :8080  # Mac/Linux
  netstat -ano | findstr :8080  # Windows
  ```

### 4. 컴파일 오류
```bash
# 클린 빌드
./gradlew clean build

# 캐시 삭제
rm -rf ~/.gradle/caches/
```

## 로그 확인

### 애플리케이션 로그
```bash
tail -f logs/moa.log
tail -f logs/security.log
```

### Tomcat 로그
```bash
tail -f $TOMCAT_HOME/logs/catalina.out
```

### Redis 모니터링
```bash
redis-cli monitor
```

## 개발 환경 vs 운영 환경

현재 설정은 **개발 환경**입니다:
- HTTP 사용 (HTTPS 비활성화)
- Cookie Secure: false
- Rate Limiting: 느슨함 (100 req/sec)
- 로그 레벨: DEBUG

운영 환경 배포시 변경 필요:
- HTTPS 활성화
- Cookie Secure: true  
- JWT Secret 변경
- DB 접속 정보 변경
- Rate Limiting 강화
