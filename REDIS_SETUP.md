# Redis Setup Guide

## 1. Redis 설치

### Mac OS
```bash
# Homebrew 사용
brew install redis

# 시작
brew services start redis

# 또는 수동 실행
redis-server
```

### Windows
1. https://github.com/microsoftarchive/redis/releases 에서 다운로드
2. 설치 후 서비스로 실행
3. 또는 WSL2 사용 권장

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis
sudo systemctl enable redis
```

## 2. Redis 확인
```bash
# Redis CLI 접속
redis-cli

# 연결 테스트
127.0.0.1:6379> ping
PONG

# 모든 키 조회
127.0.0.1:6379> keys *

# 특정 패턴 키 조회
127.0.0.1:6379> keys refresh_token:*
127.0.0.1:6379> keys blacklist:*

# 키 삭제
127.0.0.1:6379> del key_name

# 전체 초기화 (주의!)
127.0.0.1:6379> flushall
```

## 3. Redis 모니터링
```bash
# 실시간 명령어 모니터링
redis-cli monitor

# 통계 확인
redis-cli info stats

# 메모리 사용량
redis-cli info memory
```

## 4. 애플리케이션 연동 확인

### Redis가 정상 작동하는지 확인:
```java
// RedisTokenService의 isRedisAvailable() 호출
GET /api/admin/security/redis/health
```

### 토큰 저장 확인:
```bash
# 로그인 후
redis-cli
127.0.0.1:6379> keys refresh_token:*
1) "refresh_token:uuid-xxxxx-xxxxx"

127.0.0.1:6379> ttl refresh_token:uuid-xxxxx-xxxxx
604800  # 7일(초)
```

### 블랙리스트 확인:
```bash
# 로그아웃 후
127.0.0.1:6379> keys blacklist:access:*
1) "blacklist:access:jwt-token-string"

127.0.0.1:6379> ttl blacklist:access:jwt-token-string
600  # 남은 TTL (초)
```

## 5. 문제 해결

### Redis 연결 실패시:
1. Redis 서비스 실행 확인
2. 방화벽/포트 확인 (6379)
3. application.properties의 redis.host, redis.port 확인

### 메모리 부족시:
```bash
# maxmemory 설정
redis-cli config set maxmemory 256mb
redis-cli config set maxmemory-policy allkeys-lru
```

### 영속성 설정 (선택사항):
```bash
# RDB 스냅샷 설정
redis-cli config set save "900 1 300 10 60 10000"

# AOF 활성화
redis-cli config set appendonly yes
```
