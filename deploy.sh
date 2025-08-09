#!/bin/bash

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}    MOA 프로젝트 Docker 배포${NC}"
echo -e "${GREEN}========================================${NC}"

# 1. 프로젝트 빌드
echo -e "\n${YELLOW}[1/4] 프로젝트 빌드 중...${NC}"
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo -e "${RED}빌드 실패! 빌드 로그를 확인하세요.${NC}"
    exit 1
fi

# WAR 파일 확인
WAR_FILE=$(ls build/libs/*.war 2>/dev/null | head -n 1)
if [ -n "$WAR_FILE" ]; then
    echo -e "${GREEN}✓ WAR 파일 빌드 완료: $(basename $WAR_FILE)${NC}"
else
    echo -e "${RED}✗ WAR 파일을 찾을 수 없습니다.${NC}"
    exit 1
fi

# 2. 기존 컨테이너 정리
echo -e "\n${YELLOW}[2/4] 기존 컨테이너 정리 중...${NC}"
docker-compose down
echo -e "${GREEN}✓ 완료${NC}"

# 3. 컨테이너 시작
echo -e "\n${YELLOW}[3/4] Docker 컨테이너 시작 중...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}컨테이너 시작 실패!${NC}"
    docker-compose logs
    exit 1
fi

# MySQL 시작 대기
echo -e "${BLUE}서비스 시작 대기중... (15초)${NC}"
sleep 15

# 4. Firebase 파일 복사
echo -e "\n${YELLOW}Firebase 키 파일 설정 중...${NC}"
docker exec moa-tomcat mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/firebase
docker cp src/main/resources/firebase/moa-firebase-service-account-key.json moa-tomcat:/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/firebase/ 2>/dev/null || echo "Firebase 키 파일이 없습니다"

# 5. 헬스 체크
echo -e "\n${YELLOW}[4/4] 서비스 상태 확인 중...${NC}"
echo "Tomcat 시작 대기중... (최대 60초)"

for i in {1..60}; do
    if curl -f http://localhost:8080/ &>/dev/null; then
        echo -e "\n${GREEN}✓ 서비스가 정상적으로 시작되었습니다!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 최종 상태 출력
echo -e "\n${YELLOW}=== 컨테이너 상태 ===${NC}"
docker-compose ps

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}    배포 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "접속 정보:"
echo "  - 서버 내부: http://localhost:8080/api/member/all"
echo "  - 외부 접속: http://서버IP/api/member/all"
echo ""
echo "데이터베이스 접속:"
echo "  docker exec -it moa-mysql mysql -uroot -p moa"
echo ""
echo "로그 확인:"
echo "  docker-compose logs -f tomcat"
echo ""