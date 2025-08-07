-- 초기화
SET FOREIGN_KEY_CHECKS = 0;

-- DROP TABLES (FK 자식부터 → 부모순)
DROP TABLE IF EXISTS TRIP_RECORD_IMAGES;
DROP TABLE IF EXISTS TRIP_RECORDS;
DROP TABLE IF EXISTS SETTLEMENT_NOTES;
DROP TABLE IF EXISTS EXPENSE;
DROP TABLE IF EXISTS REST_TIME_SLOT;
DROP TABLE IF EXISTS REST_RES;
DROP TABLE IF EXISTS RESTAURANT_INFO;
DROP TABLE IF EXISTS TRAN_RES;
DROP TABLE IF EXISTS TRANSPORT_INFO;
DROP TABLE IF EXISTS ACCOM_RES;
DROP TABLE IF EXISTS ROOM_INFO;
DROP TABLE IF EXISTS ACCOMMODATION_INFO;
DROP TABLE IF EXISTS RESERVATION;
DROP TABLE IF EXISTS TRIP_DAY;
DROP TABLE IF EXISTS TRIP_MEMBER;
DROP TABLE IF EXISTS TRIP_LOCATION;
DROP TABLE IF EXISTS TRIP;
DROP TABLE IF EXISTS PAYMENT_RECORD;
DROP TABLE IF EXISTS ACCOUNT;
DROP TABLE IF EXISTS DRIVER_LICENSE;
DROP TABLE IF EXISTS ID_CARD;
DROP TABLE IF EXISTS NOTIFICATION;
DROP TABLE IF EXISTS OWNER;
DROP TABLE IF EXISTS MEMBER;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================================================
-- 회원 테이블
-- ========================================================================================
CREATE TABLE MEMBER
(
    member_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_type    ENUM ('ROLE_USER', 'ROLE_OWNER', 'ROLE_ADMIN') NOT NULL,
    email          VARCHAR(255)                                   NOT NULL UNIQUE,
    password       VARCHAR(255)                                   NOT NULL,
    name           VARCHAR(20)                                    NOT NULL,
    fcm_token      VARCHAR(255)                                   NULL,
    id_card_number VARCHAR(255)                                   NOT NULL UNIQUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========================================================================================
-- 사업자 테이블
-- ========================================================================================
CREATE TABLE owner (
                       business_id     VARCHAR(255)   NOT NULL,
                       business_kind   ENUM(
                        'TRANSPORT',
                        'ACCOMMODATION',
                        'RESTAURANT'
                    ) NOT NULL,
                       member_id       BIGINT         NOT NULL,
                       created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

    -- composite PK
                       PRIMARY KEY (business_id, business_kind),

    -- member 테이블의 FK
                       CONSTRAINT fk_owner_member
                           FOREIGN KEY (member_id)
                               REFERENCES member(member_id)
                               ON DELETE CASCADE
);

-- ========================================================================================
-- 주민등록증 테이블
-- ========================================================================================
CREATE TABLE ID_CARD
(
    id_card_id     BIGINT AUTO_INCREMENT,
    member_id      BIGINT UNIQUE,
    id_card_number CHAR(13)     NOT NULL,
    name           VARCHAR(20)  NOT NULL,
    issued_date    DATE         NOT NULL,
    address        VARCHAR(255) NOT NULL,
    image_url      VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_card_id),
    UNIQUE (id_card_number),
    CONSTRAINT FK_MEMBER_TO_ID_CARD_1
        FOREIGN KEY (member_id)
            REFERENCES MEMBER (member_id)
);

-- ========================================================================================
-- 운전면허증 테이블
-- ========================================================================================
CREATE TABLE DRIVER_LICENSE
(
    license_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT,
    id_card_number CHAR(13) NOT NULL UNIQUE,
    license_number CHAR(12) NOT NULL UNIQUE,
    license_type   VARCHAR(20),
    image_url      VARCHAR(255),
    issued_date    DATE,
    expiry_date    DATE,
    issuing_agency VARCHAR(50),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

-- ========================================================================================
-- 계좌 테이블
-- ========================================================================================
CREATE TABLE ACCOUNT
(
    account_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id        BIGINT,
    name             VARCHAR(20)    NOT NULL,
    account_number   VARCHAR(50)    NOT NULL UNIQUE,
    account_password VARCHAR(255)   NOT NULL,
    bank_name        ENUM ('KB')    NOT NULL,
    balance          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    is_active        BOOLEAN                 DEFAULT TRUE,
    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

-- ========================================================================================
-- 여행 테이블
-- ========================================================================================
CREATE TABLE TRIP
(
    trip_id       BIGINT AUTO_INCREMENT PRIMARY KEY,                               -- 여행 ID (기본키)
    member_id     BIGINT                              NOT NULL,                    -- 생성자 ID (외래키)
    trip_name     VARCHAR(255),                                                    -- 여행 이름 (nullable)
    trip_location ENUM ('BUSAN', 'GANGNEUNG', 'JEJU', 'SEOUL') NOT NULL,                    -- 여행 지역 (ENUM)
    start_date    DATE                                NOT NULL,                    -- 시작일
    end_date      DATE                                NOT NULL,                    -- 종료일
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 생성일시
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)                          -- MEMBER 테이블의 PK 참조
);

-- ========================================================================================
-- 여행 위치 테이블
-- ========================================================================================
CREATE TABLE TRIP_LOCATION
(
    location_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 위치 ID (기본키)
    location_name      ENUM ('BUSAN', 'GANGNEUNG', 'JEJU', 'SEOUL' ) NOT NULL,                    -- 여행 지역 (ENUM)
    latitude      DECIMAL(10, 8),                             -- 위도
    longitude     DECIMAL(11, 8),                             -- 경도
    address       VARCHAR(200)                                -- 주소
);

-- ========================================================================================
-- 여행 멤버 테이블
-- ========================================================================================
CREATE TABLE TRIP_MEMBER
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY, -- 여행 멤버 그룹 PK
    trip_id   BIGINT                  NOT NULL,  -- 여행 ID (FK → TRIP)
    member_id BIGINT                  NOT NULL,  -- 사용자 ID (FK → MEMBER)

    role      ENUM ('HOST', 'MEMBER') NOT NULL,  -- 역할 (host: 생성자, member: 동행자)
    joined_at DATETIME                NOT NULL,  -- 생성일시

    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id),
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id),

    -- 하나의 여행에 동일한 사용자가 여러 번 등록되지 않도록 제약 조건 추가
    UNIQUE KEY unique_trip_member (trip_id, member_id)
);

-- ========================================================================================
-- 여행 날짜 테이블
-- ========================================================================================
CREATE TABLE TRIP_DAY
(
    trip_day_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 여행 날짜 ID (기본키)
    trip_id     BIGINT NOT NULL,                            -- 여행 ID (외래키)
    day         DATE   NOT NULL,                            -- 여행 날짜

    -- 외래키 제약 조건: 여행이 삭제되면 날짜도 함께 삭제
    CONSTRAINT fk_trip_day_trip_id
        FOREIGN KEY (trip_id)
            REFERENCES TRIP (trip_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 결제 내역 테이블
-- ========================================================================================
CREATE TABLE PAYMENT_RECORD
(
    record_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT         NOT NULL,
    member_id        BIGINT         NOT NULL,
    trip_day_id      BIGINT         NULL,
    payment_name     VARCHAR(100)   NOT NULL,
    payment_price    DECIMAL(15, 2) NOT NULL,
    payment_date     DATETIME       NOT NULL,
    payment_location VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES ACCOUNT (account_id),
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id),
    FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY (trip_day_id)
);

-- ========================================================================================
-- 예약 테이블
-- ========================================================================================
CREATE TABLE RESERVATION
(
    reservation_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 예약 ID (기본키)
    trip_day_id    BIGINT NOT NULL,                            -- 여행 날짜 ID (외래키)
    res_kind       ENUM ('TRANSPORT', 'ACCOMMODATION', 'RESTAURANT'), -- 예약 종류

    CONSTRAINT fk_reservation_trip_day_id
        FOREIGN KEY (trip_day_id)
            REFERENCES TRIP_DAY (trip_day_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 숙박 정보 테이블
-- ========================================================================================
CREATE TABLE ACCOMMODATION_INFO
(
    accom_id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,               -- 숙박 ID (기본키)
    hotel_name      VARCHAR(255) NOT NULL,                                          -- 호텔 이름
    address         VARCHAR(255) NOT NULL,                                          -- 위치 (주소)
    location        ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,                    -- 여행 지역 (ENUM)
    latitude        DECIMAL(10, 7),                                                 -- 위도
    longitude       DECIMAL(10, 7),                                                 -- 경도
    description     TEXT,                                                           -- 설명
    hotel_image_url VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                            -- 생성일시
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 수정일시
);

-- ========================================================================================
-- 숙박 예약 테이블
-- ========================================================================================
CREATE TABLE ACCOM_RES
(
    accom_res_id   BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    accom_id       BIGINT         NOT NULL,
    reservation_id BIGINT         NULL,
    trip_day_id    BIGINT         NULL,
    guests         INT            NULL,
    hotel_name     VARCHAR(255)   NOT NULL,
    address        VARCHAR(255)   NOT NULL,
    price          DECIMAL(10,2)  NOT NULL,
    room_type      VARCHAR(50)    NOT NULL,
    room_image_url VARCHAR(255)   NULL,
    checkin_day    DATETIME       NOT NULL,
    checkout_day   DATETIME       NOT NULL,
    max_guests     INT            NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status         ENUM('AVAILABLE', 'PENDING', 'CONFIRMED') NOT NULL DEFAULT 'AVAILABLE',

    FOREIGN KEY (accom_id) REFERENCES ACCOMMODATION_INFO (accom_id),
    FOREIGN KEY (reservation_id) REFERENCES RESERVATION (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY (trip_day_id)
);

-- ========================================================================================
-- 교통 정보 테이블
-- ========================================================================================
CREATE TABLE TRANSPORT_INFO
(
    transport_id   BIGINT                                              NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 교통 ID (기본키)
    departure_id   VARCHAR(30)                                         NOT NULL,                            -- 출발역 ID
    arrival_id     VARCHAR(30)                                         NOT NULL,                            -- 도착역 ID
    departure_name VARCHAR(100),                                                                            -- 출발역 이름
    arrival_name   VARCHAR(100),                                                                            -- 도착역 이름
    departure_time DATETIME                                            NOT NULL,                            -- 출발 시각
    arrival_time   DATETIME                                            NOT NULL,                            -- 도착 시각
    train_type     ENUM ('ktx', 'ktx-sancheon', 'ktx-eum')             NOT NULL,                            -- 열차 종류
    train_no       VARCHAR(30)                                         NOT NULL,                            -- 열차 번호
    seat_type      ENUM ('general', 'first_class', 'silent', 'family') NOT NULL,                            -- 좌석 종류
    seat_total     INT                                                 NOT NULL,                            -- 총 좌석 수
    seat_remain    INT                                                 NOT NULL,                            -- 남은 좌석 수
    price          DECIMAL(15, 2)                                      NOT NULL,                            -- 가격
    is_visible     BOOLEAN                                             NOT NULL                             -- 표시 여부
);

-- ========================================================================================
-- 교통 예약 테이블
-- ========================================================================================
CREATE TABLE TRAN_RES
(
    tran_res_id    BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 교통 예약 ID (기본키)
    transport_id   BIGINT   NOT NULL,                            -- 교통 ID (외래키)
    reservation_id BIGINT   NULL,                                -- 예약 ID (외래키)
    trip_day_id    BIGINT   NULL,                                -- 여행 날짜 ID (외래키)
    departure   VARCHAR(30)   NOT NULL,                          -- 출발역
    arrival     VARCHAR(30)   NOT NULL,                          -- 도착역
    seat_room_no   INT        NOT NULL,                          -- 예약한 호실 번호
    seat_number    VARCHAR(10)   NOT NULL,                       -- 예약 좌석 번호
    seat_type      ENUM ('general', 'first_class', 'silent', 'family') NOT NULL,   -- 좌석 종류
    booked_at      DATETIME   NULL,                              -- 예약 일자
    price          DECIMAL(15, 2)   NOT NULL,                    -- 가격
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,          -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정일시
    status ENUM('AVAILABLE', 'PENDING', 'CONFIRMED') NOT NULL DEFAULT 'AVAILABLE',

    FOREIGN KEY (transport_id) REFERENCES TRANSPORT_INFO (transport_id) ON DELETE CASCADE,

    CONSTRAINT fk_tran_res_reservation_id
        FOREIGN KEY (reservation_id)
            REFERENCES RESERVATION (reservation_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_tran_res_trip_day_id
        FOREIGN KEY (trip_day_id)
            REFERENCES TRIP_DAY (trip_day_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 식당 정보 테이블
-- ========================================================================================
CREATE TABLE RESTAURANT_INFO
(
    rest_id        BIGINT                                                                        NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 식당 ID (PK)
    rest_name      VARCHAR(255)                                                                  NOT NULL,                            -- 식당 이름
    address        VARCHAR(255)                                                                  NOT NULL,                            -- 주소
    category       ENUM ('korean', 'chinese', 'japanese', 'western', 'etc')                      NOT NULL,                            -- 카테고리
    rest_image_url VARCHAR(255)                                                                  NOT NULL,                            -- 식당 이미지
    phone          VARCHAR(20)                                                                   NULL,                                -- 전화번호
    description    TEXT                                                                          NULL,                                -- 설명
    latitude       DECIMAL(10, 7)                                                                NULL,                                -- 위도
    longitude      DECIMAL(10, 7)                                                                NULL,                                -- 경도
    menu_url       VARCHAR(255)                                                                  NULL,                                -- 메뉴 URL
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                                                                               -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP                                                    -- 수정일시
);

-- ========================================================================================
-- 식당 시간 테이블
-- ========================================================================================
CREATE TABLE REST_TIME_SLOT
(
    rest_time_id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 식당 시간 ID (PK)
    rest_id        BIGINT       NOT NULL, -- 식당 ID (FK)
    res_time       VARCHAR(10)  NOT NULL, -- 식당 예약 시간
    max_capacity   INT          NOT NULL DEFAULT 10, -- 최대 예약 인원

    FOREIGN KEY (rest_id) REFERENCES RESTAURANT_INFO (rest_id)
);

-- ========================================================================================
-- 식당 예약 테이블
-- ========================================================================================
CREATE TABLE REST_RES
(
    rest_res_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,                        -- 식당 예약 ID (PK)
    rest_id        BIGINT       NOT NULL,                                                   -- 식당 ID (FK)
    reservation_id BIGINT       NOT NULL,                                                   -- 예약 ID (FK)
    trip_day_id    BIGINT       NOT NULL,                                                   -- 여행 날짜 ID (FK)
    res_num        INT          NOT NULL,                                                   -- 예약 인원
    rest_time_id    BIGINT       NOT NULL,                                                   -- 예약 시간
    status         ENUM('reserved', 'checked_in', 'completed') DEFAULT 'reserved' NOT NULL, -- 예약 상태
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                                     -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,         -- 수정일시

    FOREIGN KEY (rest_id) REFERENCES RESTAURANT_INFO (rest_id),
    FOREIGN KEY (reservation_id) REFERENCES RESERVATION (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY (trip_day_id),
    FOREIGN KEY (rest_time_id) REFERENCES REST_TIME_SLOT (rest_time_id)
);

-- ========================================================================================
-- 비용 테이블
-- ========================================================================================
CREATE TABLE EXPENSE
(
    expense_id           BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,                            -- 비용 ID (기본키)
    trip_id              BIGINT                              NOT NULL,                                                       -- 여행 ID (외래키)
    member_id            BIGINT                              NOT NULL,                                                       -- 결제자 ID (외래키)
    expense_name         VARCHAR(100)                        NOT NULL,                                                       -- 결제명
    expense_date         DATETIME                            NOT NULL,
    amount               DECIMAL(15, 2)                      NOT NULL,                                                       -- 금액 (소수점 2자리)
    location             ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,                                                       -- 결제 위치
    settlement_completed BOOLEAN                             NOT NULL,                                                       -- 정산 완료 여부
    created_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 작성일시
    updated_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id)                                                                          -- 여행 ID 외래키
        ON DELETE CASCADE
);

-- ========================================================================================
-- 알림 테이블
-- ========================================================================================
CREATE TABLE NOTIFICATION
(
    notification_id   BIGINT                                                                              NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 알림 ID (PK)
    member_id         BIGINT                                                                              NOT NULL,                            -- 수신자 ID (FK)
    trip_id           BIGINT       NULL,
    expense_id        BIGINT       NULL,
    notification_type ENUM ('TRIP', 'SETTLE') NOT NULL,                                -- 알림 유형
    sender_name       VARCHAR(100) NOT NULL,
    trip_name         VARCHAR(255) NOT NULL,
    title             VARCHAR(100)                                                                        NULL,                                -- 알림 제목
    content           TEXT                                                                                NULL,                                -- 알림 내용
    is_read           BOOLEAN                                                                             NOT NULL DEFAULT FALSE,              -- 읽음 여부
    created_at        TIMESTAMP                                                                           NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 생성일시 (자동 등록)

    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id),                                                                                      -- 수신자 ID 외래키
    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id),
    FOREIGN KEY (expense_id) REFERENCES EXPENSE (expense_id)
);

-- ========================================================================================
-- 정산 내역 테이블
-- ========================================================================================
CREATE TABLE SETTLEMENT_NOTES
(
    settlement_id BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,                            -- 정산 ID (기본키)
    expense_id    BIGINT         NOT NULL,                                                       -- 비용 ID (외래키)
    trip_id       BIGINT         NOT NULL,                                                       -- 여행 ID (외래키)
    member_id     VARCHAR(36)    NOT NULL,                                                       -- 사용자 ID (외래키, UUID 형식으로 추정)
    share_amount  DECIMAL(15, 2) NOT NULL,                                                       -- 정산 금액
    is_payed      BOOLEAN        NOT NULL,                                                       -- 정산 여부 (true/false)
    received      BOOLEAN        NOT NULL,                                                       -- 받은 요청인지 여부(true/false)
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 작성일시
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    FOREIGN KEY (expense_id) REFERENCES EXPENSE (expense_id)                                     -- 비용 ID 외래키
        ON DELETE CASCADE,

    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id)                                              -- 여행 ID 외래키
        ON DELETE CASCADE
);

-- ========================================================================================
-- 여행 기록 테이블
-- ========================================================================================
CREATE TABLE TRIP_RECORDS
(
    record_id   BIGINT AUTO_INCREMENT PRIMARY KEY,    -- 기록 ID (PK)
    trip_id     BIGINT       NOT NULL,                -- 여행 ID (FK)
    member_id   BIGINT       NOT NULL,                -- 작성자 ID (FK)
    title       VARCHAR(255) NOT NULL,                -- 제목
    record_date DATE         NOT NULL,                -- 날짜
    content     TEXT,                                 -- 내용
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 작성일시
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    -- 여행 삭제되면 해당 여행기록도 같이 삭제
    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_record_to_member FOREIGN KEY (member_id) REFERENCES MEMBER (member_id),

    -- 성능 향상을 위한 인덱스 (Indexes) -> 일자별 여행기록 조회 기능
    INDEX idx_trip_id_record_date (trip_id, record_date)
);

-- ========================================================================================
-- 여행 기록 사진 테이블
-- ========================================================================================
CREATE TABLE TRIP_RECORD_IMAGES
(
    image_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT       NOT NULL,
    image_url VARCHAR(1024) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 작성일시
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    CONSTRAINT fk_trip_record_image_record_id_v2
        FOREIGN KEY (record_id)
            REFERENCES TRIP_RECORDS (record_id)
            ON DELETE CASCADE
);

-- 회원 테스트용 데이터
INSERT INTO MEMBER (member_id, member_type, email, password, name, fcm_token, id_card_number)
VALUES (1, 'ROLE_USER', 'karina@test.com', '1234', '카리나', 'asdf1234', '0004114000001'),
       (2, 'ROLE_USER', 'winter@test.com', '1234', '윈터', 'qwer1234', '0101014000002'),
       (3, 'ROLE_USER', 'giselle@test.com', '1234', '지젤', 'asdf5678', '0010304000003'),
       (4, 'ROLE_USER', 'ningning@test.com', '1234', '닝닝', 'qwer5678', '0210234000002');

-- 1) MEMBER 테이블에 사업자 계정 추가
INSERT INTO MEMBER (
    member_id,
    member_type,
    email,
    password,
    name,
    fcm_token,
    id_card_number
) VALUES
      (5, 'ROLE_OWNER', '123-45-67890',    '1234',  '교통사업자',       NULL, 'OWN0000001'),
      (6, 'ROLE_OWNER', '987-65-43210',    '1234',  '숙박사업자',       NULL, 'OWN0000002'),
      (7, 'ROLE_OWNER', '456-78-90123-1',    '1234',  '해운대곰장어사업자', NULL, 'OWN0000003'),
      (8, 'ROLE_OWNER', '456-78-90123-2',    '1234',  '코이이자카야사업자', NULL, 'OWN0000004'),
      (9, 'ROLE_OWNER', '456-78-90123-3',    '1234',  '팔선생중화요리사업자',NULL,'OWN0000005'),
      (10, 'ROLE_OWNER', '789-23-45678-4',    '1234',  '파스타하우스사업자',  NULL, 'OWN0000006'),
      (11, 'ROLE_OWNER', '456-78-90123-5',    '1234',  '이색분식연구소사업자',NULL,'OWN0000007')
;
-- 2) OWNER 테이블에 business ↔ member 연결
INSERT INTO OWNER (
    business_id,
    business_kind,
    member_id
) VALUES
      ( 1, 'TRANSPORT',    5),
      ( 1, 'ACCOMMODATION', 6),
      ( 1, 'RESTAURANT',   7),  -- 해운대 곰장어집
      ( 5, 'RESTAURANT',   8),  -- 코이 이자카야
      ( 7, 'RESTAURANT',   9),  -- 팔선생 중화요리
      (10, 'RESTAURANT',  10),  -- 파스타하우스
      (15, 'RESTAURANT',  11)   -- 이색분식연구소
;

-- 주민등록증 테스트용 데이터
INSERT INTO ID_CARD (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (1, '0004114000001', '카리나', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/karina.jfif');

INSERT INTO ID_CARD (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (2, '0101014000002', '윈터', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/winter.jfif');

INSERT INTO ID_CARD (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (3, '0010304000003', '지젤', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/giselle.jfif');

INSERT INTO ID_CARD (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (4, '0210234000002', '닝닝', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/ningning.jfif');


-- 운전면허증 테스트용 데이터
-- 카리나 (경남 양산 → 울산남부경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (1, '0004114000001', '201234567810', '1종 보통',
        '2020-06-15', '2030-06-15', '울산남부경찰서', 'IDphoto/karina.jfif');

-- 윈터 (부산 해운대 → 부산해운대경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (2, '0101014000002', '211198765421', '2종 소형',
        '2021-03-10', '2031-03-10', '부산해운대경찰서', 'IDphoto/winter.jfif');

-- 지젤 (도쿄 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (3, '0010304000003', '221011223309', '2종 보통',
        '2022-07-22', '2032-07-22', '서울성동경찰서', 'IDphoto/giselle.jfif');

-- 닝닝 (하얼빈 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (4, '0210234000002', '230944556633', '원동기장치자전거',
        '2023-01-05', '2033-01-05', '서울성동경찰서', 'IDphoto/ningning.jfif');


-- 계좌 테스트용 데이터
-- 카리나
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (1, '카리나', '1234567890001', '1111', 'KB', 10000000.00);

-- 윈터
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (2, '윈터', '1234567890002', '2222', 'KB', 1000000.00);

-- 지젤
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (3, '지젤', '1234567890003', '3333', 'KB', 100000.00);

-- 닝닝
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (4, '닝닝', '1234567890004', '4444', 'KB', 1000000.00);


-- 결제 내역 테스트용 데이터
-- 카리나 (member_id=1)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (1, 1, '편의점 간식', 4500.00, '2025-08-01 09:10:00', 'BUSAN'),
       (1, 1, '부산 파스타하우스', 21000.00, '2025-08-03 12:43:25', 'BUSAN'),
       (1, 1, '카페 커피', 6500.00, '2025-08-05 15:40:00', 'BUSAN');

-- 윈터 (member_id=2)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (2, 2, '주유소 결제', 54000.00, '2025-08-03 10:20:00', 'BUSAN'),
       (2, 2, '숙박 비용', 193000.00, '2025-08-03 12:00:00', 'BUSAN'),
       (2, 2, '교통비', 105000.00, '2025-08-04 13:30:00', 'BUSAN'),
       (2, 2, '길거리 간식', 9999.00, '2025-08-04 17:30:00', 'BUSAN'),
       (2, 2, '편의점 물품', 12000.00, '2025-08-05 22:10:00', 'BUSAN');

-- 지젤 (member_id=3)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (3, 3, '팔선생 중화요리', 61000.00, '2025-08-04 12:56:43', 'BUSAN'),
       (3, 3, '이색분식연구소', 10000.00, '2025-08-04 19:44:26', 'BUSAN'),
       (3, 3, '기념품 구매', 22000.00, '2025-08-04 11:15:00', 'BUSAN'),
       (3, 3, '카페 디저트', 8500.00, '2025-08-04 15:45:00', 'BUSAN');

-- 닝닝 (member_id=4)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (4, 4, '이자카야 코이', 24300.00, '2025-08-03 18:35:47', 'BUSAN'),
       (4, 4, '편의점 간식', 5500.00, '2025-08-04 12:20:00', 'BUSAN'),
       (4, 4, '기념품', 17000.00, '2025-08-05 16:30:00', 'BUSAN');

-- 여행 테스트용 데이터(카리나 1인여행 8/1 ~ 8/4)
INSERT INTO TRIP (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '혼자 부산 여행', 'BUSAN', '2025-08-01', '2025-08-04');

-- 여행 테스트용 데이터(윈터, 지젤, 닝닝 3인여행 8/3 ~ 8/5)
INSERT INTO TRIP (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '셋이서 부산 여행', 'BUSAN', '2025-08-03', '2025-08-05');



INSERT INTO TRIP_LOCATION (location_name, latitude, longitude, address)
VALUES ('BUSAN', 35.179554, 129.075642, '부산광역시 중구 중앙대로 100'),
       ('GANGNEUNG', 37.751853, 128.876057, '강원특별자치도 강릉시 교동광장로 100'),
       ('JEJU', 33.499621, 126.531188, '제주특별자치도 제주시 중앙로 100'),
       ('SEOUL', 37.566535, 126.977969, '서울특별시 중구 세종대로 110');



INSERT INTO TRIP_MEMBER (trip_id, member_id, role, joined_at)
VALUES  (1, 1, 'HOST', NOW()), -- 카리나 호스트 부산여행
        (2, 2, 'HOST', NOW()), -- 윈터 호스트 부산여행
        (2, 3, 'MEMBER', NOW()), -- 닝닝 멤버 부산여행
        (2, 4, 'MEMBER', NOW()); -- 지젤 멤버 부산여행

INSERT INTO TRIP_DAY (trip_id, day)
VALUES
-- 카리나 부산 여행 (3일)
(1, '2025-08-01'), -- tripDayId = 1
(1, '2025-08-02'), -- 2
(1, '2025-08-03'), -- 3
(1, '2025-08-04'), -- 4
-- 윈닝젤 부산 여행 (3일)
(2, '2025-08-03'), -- 5
(2, '2025-08-04'), -- 6
(2, '2025-08-05'); -- 7

INSERT INTO RESERVATION (trip_day_id, res_kind)
VALUES
-- trip_day_id: 1, 2, 3, 4 => 카리나 부산 여행
(1,'ACCOMMODATION'), -- 숙박
(2,'ACCOMMODATION'), -- 숙박
(3,'ACCOMMODATION'), -- 숙박
(4,'ACCOMMODATION'), -- 숙박
-- trip_day_id : 5, 6, 7 => 윈닝젤 부산 여행
(5,'ACCOMMODATION'), -- 숙박
(6,'ACCOMMODATION'), -- 숙박
(7,'ACCOMMODATION'), -- 숙박
-- 교통 데이터 추가
(1, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/1 서울역 -> 부산역 10:00 출발 reservationId = 8 , tripDayId = 1
(4, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/4 부산역 -> 서울역 16:00 출발 reservationId = 9, tripDayId = 4
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/3 서울역 -> 부산역 11:00 출발 (윈터, 지젤) reservationId = 10, tripDayId = 5
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/5 서울역 -> 부산역 12:00 출발 (닝닝) reservationId = 11, tripDayId = 5
(7, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/7 부산역 -> 서울역 10:00 출발 (윈터 스케줄 바쁨) reservationId = 12, tripDayId = 7
(7, 'TRANSPORT'); -- (윈터, 지젤, 닝닝 부산여행) 8/7 부산역 -> 서울역 15:00 출발 (지젤, 닝닝 느긋) reservationId = 13, tripDayId = 7

-- 교통 예약 테스트 데이터
INSERT INTO transport_info (
    departure_id, arrival_id, departure_name, arrival_name,
    departure_time, arrival_time, train_type, train_no,
    seat_type, seat_total, seat_remain, price, is_visible
) VALUES
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 10:00:00', '2025-08-01 12:30:00', 'ktx', 'KTX-801', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 10:00:00', '2025-08-01 12:30:00', 'ktx-sancheon', 'KTX-802', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 11:00:00', '2025-08-01 13:30:00', 'ktx', 'KTX-803', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 11:00:00', '2025-08-01 13:30:00', 'ktx-sancheon', 'KTX-804', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 12:00:00', '2025-08-01 14:30:00', 'ktx', 'KTX-805', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 12:00:00', '2025-08-01 14:30:00', 'ktx-sancheon', 'KTX-806', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 13:00:00', '2025-08-01 15:30:00', 'ktx', 'KTX-807', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 13:00:00', '2025-08-01 15:30:00', 'ktx-sancheon', 'KTX-808', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 14:00:00', '2025-08-01 16:30:00', 'ktx', 'KTX-809', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 14:00:00', '2025-08-01 16:30:00', 'ktx-sancheon', 'KTX-810', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 15:00:00', '2025-08-01 17:30:00', 'ktx', 'KTX-811', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 15:00:00', '2025-08-01 17:30:00', 'ktx-sancheon', 'KTX-812', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 16:00:00', '2025-08-01 18:30:00', 'ktx', 'KTX-813', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 16:00:00', '2025-08-01 18:30:00', 'ktx-sancheon', 'KTX-814', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 17:00:00', '2025-08-01 19:30:00', 'ktx', 'KTX-815', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 17:00:00', '2025-08-01 19:30:00', 'ktx-sancheon', 'KTX-816', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 10:00:00', '2025-08-02 12:30:00', 'ktx', 'KTX-817', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 10:00:00', '2025-08-02 12:30:00', 'ktx-sancheon', 'KTX-818', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 11:00:00', '2025-08-02 13:30:00', 'ktx', 'KTX-819', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 11:00:00', '2025-08-02 13:30:00', 'ktx-sancheon', 'KTX-820', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 12:00:00', '2025-08-02 14:30:00', 'ktx', 'KTX-821', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 12:00:00', '2025-08-02 14:30:00', 'ktx-sancheon', 'KTX-822', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 13:00:00', '2025-08-02 15:30:00', 'ktx', 'KTX-823', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 13:00:00', '2025-08-02 15:30:00', 'ktx-sancheon', 'KTX-824', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 14:00:00', '2025-08-02 16:30:00', 'ktx', 'KTX-825', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 14:00:00', '2025-08-02 16:30:00', 'ktx-sancheon', 'KTX-826', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 15:00:00', '2025-08-02 17:30:00', 'ktx', 'KTX-827', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 15:00:00', '2025-08-02 17:30:00', 'ktx-sancheon', 'KTX-828', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 16:00:00', '2025-08-02 18:30:00', 'ktx', 'KTX-829', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 16:00:00', '2025-08-02 18:30:00', 'ktx-sancheon', 'KTX-830', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 17:00:00', '2025-08-02 19:30:00', 'ktx', 'KTX-831', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 17:00:00', '2025-08-02 19:30:00', 'ktx-sancheon', 'KTX-832', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 10:00:00', '2025-08-03 12:30:00', 'ktx', 'KTX-833', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 10:00:00', '2025-08-03 12:30:00', 'ktx-sancheon', 'KTX-834', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 11:00:00', '2025-08-03 13:30:00', 'ktx', 'KTX-835', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 11:00:00', '2025-08-03 13:30:00', 'ktx-sancheon', 'KTX-836', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 12:00:00', '2025-08-03 14:30:00', 'ktx', 'KTX-837', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 12:00:00', '2025-08-03 14:30:00', 'ktx-sancheon', 'KTX-838', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 13:00:00', '2025-08-03 15:30:00', 'ktx', 'KTX-839', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 13:00:00', '2025-08-03 15:30:00', 'ktx-sancheon', 'KTX-840', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 14:00:00', '2025-08-03 16:30:00', 'ktx', 'KTX-841', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 14:00:00', '2025-08-03 16:30:00', 'ktx-sancheon', 'KTX-842', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 15:00:00', '2025-08-03 17:30:00', 'ktx', 'KTX-843', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 15:00:00', '2025-08-03 17:30:00', 'ktx-sancheon', 'KTX-844', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 16:00:00', '2025-08-03 18:30:00', 'ktx', 'KTX-845', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 16:00:00', '2025-08-03 18:30:00', 'ktx-sancheon', 'KTX-846', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 17:00:00', '2025-08-03 19:30:00', 'ktx', 'KTX-847', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 17:00:00', '2025-08-03 19:30:00', 'ktx-sancheon', 'KTX-848', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 10:00:00', '2025-08-04 12:30:00', 'ktx', 'KTX-849', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 10:00:00', '2025-08-04 12:30:00', 'ktx-sancheon', 'KTX-850', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 11:00:00', '2025-08-04 13:30:00', 'ktx', 'KTX-851', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 11:00:00', '2025-08-04 13:30:00', 'ktx-sancheon', 'KTX-852', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 12:00:00', '2025-08-04 14:30:00', 'ktx', 'KTX-853', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 12:00:00', '2025-08-04 14:30:00', 'ktx-sancheon', 'KTX-854', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 13:00:00', '2025-08-04 15:30:00', 'ktx', 'KTX-855', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 13:00:00', '2025-08-04 15:30:00', 'ktx-sancheon', 'KTX-856', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 14:00:00', '2025-08-04 16:30:00', 'ktx', 'KTX-857', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 14:00:00', '2025-08-04 16:30:00', 'ktx-sancheon', 'KTX-858', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 15:00:00', '2025-08-04 17:30:00', 'ktx', 'KTX-859', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 15:00:00', '2025-08-04 17:30:00', 'ktx-sancheon', 'KTX-860', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 16:00:00', '2025-08-04 18:30:00', 'ktx', 'KTX-861', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 16:00:00', '2025-08-04 18:30:00', 'ktx-sancheon', 'KTX-862', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 17:00:00', '2025-08-04 19:30:00', 'ktx', 'KTX-863', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 17:00:00', '2025-08-04 19:30:00', 'ktx-sancheon', 'KTX-864', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 10:00:00', '2025-08-05 12:30:00', 'ktx', 'KTX-865', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 10:00:00', '2025-08-05 12:30:00', 'ktx-sancheon', 'KTX-866', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 11:00:00', '2025-08-05 13:30:00', 'ktx', 'KTX-867', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 11:00:00', '2025-08-05 13:30:00', 'ktx-sancheon', 'KTX-868', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 12:00:00', '2025-08-05 14:30:00', 'ktx', 'KTX-869', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 12:00:00', '2025-08-05 14:30:00', 'ktx-sancheon', 'KTX-870', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 13:00:00', '2025-08-05 15:30:00', 'ktx', 'KTX-871', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 13:00:00', '2025-08-05 15:30:00', 'ktx-sancheon', 'KTX-872', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 14:00:00', '2025-08-05 16:30:00', 'ktx', 'KTX-873', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 14:00:00', '2025-08-05 16:30:00', 'ktx-sancheon', 'KTX-874', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 15:00:00', '2025-08-05 17:30:00', 'ktx', 'KTX-875', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 15:00:00', '2025-08-05 17:30:00', 'ktx-sancheon', 'KTX-876', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 16:00:00', '2025-08-05 18:30:00', 'ktx', 'KTX-877', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 16:00:00', '2025-08-05 18:30:00', 'ktx-sancheon', 'KTX-878', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 17:00:00', '2025-08-05 19:30:00', 'ktx', 'KTX-879', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 17:00:00', '2025-08-05 19:30:00', 'ktx-sancheon', 'KTX-880', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 10:00:00', '2025-08-06 12:30:00', 'ktx', 'KTX-881', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 10:00:00', '2025-08-06 12:30:00', 'ktx-sancheon', 'KTX-882', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 11:00:00', '2025-08-06 13:30:00', 'ktx', 'KTX-883', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 11:00:00', '2025-08-06 13:30:00', 'ktx-sancheon', 'KTX-884', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 12:00:00', '2025-08-06 14:30:00', 'ktx', 'KTX-885', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 12:00:00', '2025-08-06 14:30:00', 'ktx-sancheon', 'KTX-886', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 13:00:00', '2025-08-06 15:30:00', 'ktx', 'KTX-887', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 13:00:00', '2025-08-06 15:30:00', 'ktx-sancheon', 'KTX-888', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 14:00:00', '2025-08-06 16:30:00', 'ktx', 'KTX-889', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 14:00:00', '2025-08-06 16:30:00', 'ktx-sancheon', 'KTX-890', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 15:00:00', '2025-08-06 17:30:00', 'ktx', 'KTX-891', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 15:00:00', '2025-08-06 17:30:00', 'ktx-sancheon', 'KTX-892', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 16:00:00', '2025-08-06 18:30:00', 'ktx', 'KTX-893', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 16:00:00', '2025-08-06 18:30:00', 'ktx-sancheon', 'KTX-894', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 17:00:00', '2025-08-06 19:30:00', 'ktx', 'KTX-895', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 17:00:00', '2025-08-06 19:30:00', 'ktx-sancheon', 'KTX-896', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 10:00:00', '2025-08-07 12:30:00', 'ktx', 'KTX-897', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 10:00:00', '2025-08-07 12:30:00', 'ktx-sancheon', 'KTX-898', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 11:00:00', '2025-08-07 13:30:00', 'ktx', 'KTX-899', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 11:00:00', '2025-08-07 13:30:00', 'ktx-sancheon', 'KTX-900', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 12:00:00', '2025-08-07 14:30:00', 'ktx', 'KTX-901', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 12:00:00', '2025-08-07 14:30:00', 'ktx-sancheon', 'KTX-902', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 13:00:00', '2025-08-07 15:30:00', 'ktx', 'KTX-903', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 13:00:00', '2025-08-07 15:30:00', 'ktx-sancheon', 'KTX-904', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 14:00:00', '2025-08-07 16:30:00', 'ktx', 'KTX-905', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 14:00:00', '2025-08-07 16:30:00', 'ktx-sancheon', 'KTX-906', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 15:00:00', '2025-08-07 17:30:00', 'ktx', 'KTX-907', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 15:00:00', '2025-08-07 17:30:00', 'ktx-sancheon', 'KTX-908', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 16:00:00', '2025-08-07 18:30:00', 'ktx', 'KTX-909', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 16:00:00', '2025-08-07 18:30:00', 'ktx-sancheon', 'KTX-910', 'general', 240, 240, 49800.00, 1),
      ('SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 17:00:00', '2025-08-07 19:30:00', 'ktx', 'KTX-911', 'general', 240, 240, 49800.00, 1),
      ('BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 17:00:00', '2025-08-07 19:30:00', 'ktx-sancheon', 'KTX-912', 'general', 240, 240, 49800.00, 1);

INSERT INTO ACCOMMODATION_INFO (hotel_name, address, location , latitude, longitude, description, hotel_image_url)
VALUES ('부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 'BUSAN',  35.1664, 129.0624,
        '중심 업무 지구의 고층 유리 건물에 자리한 이 고급 호텔은 서면역에서 도보 5분, 광안리 해수욕장에서 지하철로 33분 거리에 있습니다. \n\n아늑하고 우아한 객실에 무료 Wi-Fi, 평면 TV, 차 및 커피 메이커가 갖춰져 있습니다. 스위트룸에는 거실이 추가되며 업그레이드 스위트룸에는 사우나, 벽난로, 식탁이 마련되어 있습니다. 클럽층 객실에는 무료 조식, 스낵, 애프터눈 티가 제공됩니다. 야구를 테마로 꾸민 스위트룸이 2곳 있습니다. 룸 서비스도 이용 가능합니다. \n\n레스토랑 5곳, 베이커리, 정기 라이브 음악 공연이 열리는 바가 있습니다. 헬스장, 사우나, 골프 연습장, 실내외 수영장도 이용할 수 있습니다.',
        'https://yaimg.yanolja.com/v5/2023/01/04/10/1280/63b55a0edcb3e9.58092209.jpg'),
       ('씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 'GANGNEUNG',  37.7760, 128.9101,
        '해변가의 우아한 타워에 자리 잡고 있어 동해 바다가 바로 보이는 이 세련된 호텔은 정동진 기차역에서 6km 떨어져 있습니다.\n\n쾌적한 객실에는 평면 TV, Wi-Fi, 미니 냉장고, 유리 벽으로 된 욕실이 있으며, 대부분의 객실에서 바다 전망이 보입니다. 미니멀리즘 인테리어가 돋보이는 온돌 방식의 객실에는 이불이 제공됩니다. 품격 있는 스위트룸에는 휴식 공간이 추가되고, 업그레이드 스위트룸에는 식사 공간, 우아한 거실, 단독형 욕조를 구비한 고급 욕실이 있습니다. 룸서비스도 이용 가능합니다.\n\n세련된 레스토랑 2곳, 바다 전망이 보이는 바, 야외 인피니티 풀은 물론 실내 수영장, 헬스장, 어린이 놀이 공간과 현대적인 야외 원형 극장도 있습니다.',
        'https://yaimg.yanolja.com/v5/2025/05/02/06/1280/6814641ba896e4.56171130.jpg'),
       ('JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 'JEJU',  33.2343, 126.5347,
        '제주의 바다를 마주한 JW 메리어트 제주 리조트 & 스파는 제주 국제공항에서 50분 거리에 위치하고 있습니다. 서귀포 매일올레시장과 산방산, 성산일출봉 등 자연경관 가까이 자리 잡은 JW 메리어트 제주에서 진정한 휴식을 즐겨보세요. 올데이 다이닝 레스토랑 아일랜드 키친에서 브런치 로열과 함께 여유롭게 하루를 시작하고, 더 라운지에서 애프터눈티 세트를 경험하실 수 있습니다.\n\n더 플라잉 호그에서는 우드 파이어 그릴에 구워 낸 제주식 구이 요리를 파인 다이닝 스타일로 추천해 드립니다. SPA by JW에서 페이셜 및 딥 티슈 마사지를 경험하며 웰니스에 집중해 보는 건 어떨까요?\n\n인피니티 풀을 포함해 총 4곳의 실내 수영장 또는 실외 수영장 또한 마련되어 있습니다. 패밀리클럽에서 아이들과 즐거운 시간을 보낼 수 있고, 어린이들을 위한 다양한 키즈 액티비티 프로그램도 준비됩니다. 완벽한 비즈니스 행사와 데스티네이션 웨딩을 계획하신다면, 한식 또는 양식 옵션을 선택하실 수 있는 맞춤 케이터링 메뉴가 제공되는 JW 메리어트 제주의 실내 혹은 실외 이벤트 공간을 활용해 보세요.\n\nLED TV, 미니바, 대리석 욕조 그리고 무료 Wi-Fi가 제공되는 안락한 객실에서 충분한 휴식을 취하세요. 대부분의 객실에 아름다운 오션뷰를 만끽할 수 있는 발코니가 설치되어 있습니다. JW 메리어트 제주에서 숨이 멎을 정도로 아름다운 제주도의 풍경을 경험해보세요.',
        'https://yaimg.yanolja.com/v5/2025/07/10/09/1280/686f8a74540cf6.30391796.jpg'),
       ('시그니엘 부산', '부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워', 'BUSAN', '35.1633', '129.1637', '해운대 해변과 부산의 아름다운 스카이라인을 조망할 수 있는 럭셔리 호텔입니다. 최상의 서비스와 현대적인 시설을 자랑하며, 다양한 레스토랑과 수영장, 스파를 갖추고 있습니다.', 'https://yaimg.yanolja.com/v5/2023/01/04/10/1280/63b55a0edcb3e9.58092209.jpg'),
       ('그랜드조선 부산', '부산광역시 해운대구 해운대해변로 292', 'BUSAN', '35.1639', '129.1610',
        '해운대 해변가에 위치한 5성급 호텔로, 품격 있는 서비스와 편안한 휴식을 제공합니다. 다양한 식음료 시설과 실내외 수영장, 키즈 라운지 등을 갖추고 있어 가족 여행객에게도 인기가 많습니다.', 'https://cf.bstatic.com/xdata/images/hotel/max1024x768/274680179.jpg?k=9f32fc5cb943f6998db47daaad1044ae59a112f550c14d29913833ef9e09b803&o='),
       ('파크 하얏트 부산', '부산광역시 해운대구 마린시티1로 51', 'BUSAN', '35.1652', '129.1491', '운대 마린시티에 위치한 럭셔리 호텔로, 광안대교와 수영만 요트경기장의 전경을 감상할 수 있습니다. 고급스러운 객실과 미식 경험을 제공하는 레스토랑, 최신식 피트니스 시설을 갖추고 있습니다.', 'https://yaimg.yanolja.com/v5/2022/09/01/13/1280/6310b57ea38718.17915397.jpg');


-- 숙박 정보 테스트용 데이터
INSERT INTO ACCOM_RES (accom_res.accom_res_id,accom_id, reservation_id, trip_day_id, guests, hotel_name, address, price, room_type, room_image_url, checkin_day, checkout_day, max_guests, status)
VALUES
    -- 카리나 -> 부산 롯데(8/1) twin, 시그니엘(8/2) twin, 그랜드조선(8/3) twin, 파크 하얏트(8/4) deluxe
    -- 윈닝젤 -> 부산 롯데(8/1~8/2) family, 시그니엘(8/3) deluxe
    -- 부산 롯데 호텔(accom_id=1, 방 종류 3개, 방은 2개씩)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (1,1, 1, 1, 1, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 2, 'CONFIRMED'),
    (2,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 2, 'AVAILABLE'),
    (3,1, 5, 5, 3, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 4, 'AVAILABLE'),
    (4,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 4, 'AVAILABLE'),
    (5,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 3, 'AVAILABLE'),
    (6,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 3, 'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (7,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 2, 'AVAILABLE'),
    (8,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 2, 'AVAILABLE'),
    (9,1, 5, 6, 3, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 4, 'AVAILABLE'),
    (10,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 4, 'AVAILABLE'),
    (11,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 3, 'AVAILABLE'),
    (12,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-02 15:00:00', '2025-08-03 11:00:00', 3, 'AVAILABLE'),
    -- 8월 3일: 모든 방 AVAILABLE (검색 가능)
    (13,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 2, 'AVAILABLE'),
    (14,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 2, 'AVAILABLE'),
    (15,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 4, 'AVAILABLE'),
    (16,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 4, 'AVAILABLE'),
    (17,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 3, 'AVAILABLE'),
    (18,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-03 15:00:00', '2025-08-04 11:00:00', 3, 'AVAILABLE'),
    -- 8월 4일: 모든 방 AVAILABLE (검색 가능)
    (19,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 2, 'AVAILABLE'),
    (20,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 2, 'AVAILABLE'),
    (21,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 4, 'AVAILABLE'),
    (22,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 4, 'AVAILABLE'),
    (23,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 3, 'AVAILABLE'),
    (24,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-04 15:00:00', '2025-08-05 11:00:00', 3, 'AVAILABLE'),
    -- 8월 5일: 모든 방 AVAILABLE (검색 가능)
    (25,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 2, 'AVAILABLE'),
    (26,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 180000, 'twin', 'https://pix8.agoda.net/hotelImages/42958/1077789081/1e9796c0384a0d4d4e644146abcc44d5.jpg?ce=2&s=1024x', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 2, 'AVAILABLE'),
    (27,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 4, 'AVAILABLE'),
    (28,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 193000, 'family', 'https://pix8.agoda.net/hotelImages/42958/1077789232/439433e711e91007255552a8680dedcd.jpg?ce=2&s=1024x', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 4, 'AVAILABLE'),
    (29,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 3, 'AVAILABLE'),
    (30,1, NULL, NULL, NULL, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 210000, 'deluxe', 'https://pix8.agoda.net/hotelImages/42958/1077789167/ddbd6c29806e83f114af8ad178fc1f31.jpg?ce=2&s=208x117&ar=16x9', '2025-08-05 15:00:00', '2025-08-06 11:00:00', 3, 'AVAILABLE'),

    -- 씨마크 호텔(accom_id=2, 방 종류 3개, 방은 1개씩)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (31,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 180000, 'twin', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 2, 'AVAILABLE'),
    (32,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 193000, 'family', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 4, 'AVAILABLE'),
    (33,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 210000, 'deluxe', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 3, 'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (34,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 180000, 'twin', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 2, 'AVAILABLE'),
    (35,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 193000, 'family', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 4, 'AVAILABLE'),
    (36,2, NULL, NULL, NULL, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 210000, 'deluxe', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 3, 'AVAILABLE'),

    -- JW 메리어트 제주(accom_id=3, 방 종류 2개, 방은 2개씩)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (37,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'family', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 4, 'AVAILABLE'),
    (38,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'family', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 4, 'AVAILABLE'),
    (39,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'deluxe', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 3, 'AVAILABLE'),
    (40,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'deluxe', NULL, '2025-08-01 15:00:00', '2025-08-02 11:00:00', 3, 'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (41,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'family', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 4, 'AVAILABLE'),
    (42,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'family', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 4, 'AVAILABLE'),
    (43,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'deluxe', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 3, 'AVAILABLE'),
    (44,3, NULL, NULL, NULL, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 200000, 'deluxe', NULL, '2025-08-02 15:00:00', '2025-08-03 11:00:00', 3, 'AVAILABLE'),

    -- 시그니엘 부산(accom_id=4, 방 종류 2개 방 1개씩 8/1~8/4)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (45,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',230000,'twin','https://pix8.agoda.net/hotelImages/34297119/1150527990/a92210af40041d811b7c170cae042485.jpeg?ce=2&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',2,'AVAILABLE'),
    (46,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',300000,'deluxe','https://pix8.agoda.net/hotelImages/13870752/1077789120/935cd010a85089f350f7f866d8b3aea2.jpg?ce=2&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',2,'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (47,4,2,2,1,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',230000,'twin','https://pix8.agoda.net/hotelImages/34297119/1150527990/a92210af40041d811b7c170cae042485.jpeg?ce=2&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',2,'CONFIRMED'),
    (48,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',300000,'deluxe','https://pix8.agoda.net/hotelImages/13870752/1077789120/935cd010a85089f350f7f866d8b3aea2.jpg?ce=2&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',2,'AVAILABLE'),
    -- 8월 3일: 모든 방 AVAILABLE (검색 가능)
    (49,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',230000,'twin','https://pix8.agoda.net/hotelImages/34297119/1150527990/a92210af40041d811b7c170cae042485.jpeg?ce=2&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',2,'AVAILABLE'),
    (50,4,6,7,3,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',300000,'deluxe','https://pix8.agoda.net/hotelImages/13870752/1077789120/935cd010a85089f350f7f866d8b3aea2.jpg?ce=2&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',2,'AVAILABLE'),
    -- 8월 4일: 모든 방 AVAILABLE (검색 가능)
    (51,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',230000,'twin','https://pix8.agoda.net/hotelImages/34297119/1150527990/a92210af40041d811b7c170cae042485.jpeg?ce=2&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',2,'AVAILABLE'),
    (52,4,NULL,NULL,NULL,'시그니엘 부산','부산광역시 해운대구 달맞이길 30, 엘시티 랜드마크타워',300000,'deluxe','https://pix8.agoda.net/hotelImages/13870752/1077789120/935cd010a85089f350f7f866d8b3aea2.jpg?ce=2&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',2,'AVAILABLE'),
    -- 그랜드조선 부산(accom_id=5 방 종류 3개 방 1개씩 8/1~8/6)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (53,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',2,'AVAILABLE'),
    (54,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',250000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',4,'AVAILABLE'),
    (55,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',2,'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (56,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',2,'AVAILABLE'),
    (57,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',250000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',4,'AVAILABLE'),
    (58,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',2,'AVAILABLE'),
    -- 8월 3일: 모든 방 AVAILABLE (검색 가능)
    (59,5,3,3,1,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',2,'CONFIRMED'),
    (60,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',250000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',4,'AVAILABLE'),
    (61,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',2,'AVAILABLE'),
    -- 8월 4일: 모든 방 AVAILABLE (검색 가능)
    (62,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',2,'AVAILABLE'),
    (63,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',250000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',4,'AVAILABLE'),
    (64,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',2,'AVAILABLE'),
    -- 8월 5일: 모든 방 AVAILABLE (검색 가능)
    (65,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',2,'AVAILABLE'),
    (66,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',300000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',4,'AVAILABLE'),
    (67,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',2,'AVAILABLE'),
    -- 8월 6일: 모든 방 AVAILABLE (검색 가능)
    (68,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',180000,'twin','https://pix8.agoda.net/hotelImages/16933389/649274911/9a391dc80a143bc39b0d575b8356d483.jpg?ce=0&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',2,'AVAILABLE'),
    (69,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',300000,'family','https://pix8.agoda.net/hotelImages/16933389/-1/aaba0bc319b64912911a31693bcbea3a.jpg?ca=17&ce=1&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',4,'AVAILABLE'),
    (70,5,NULL,NULL,NULL,'그랜드조선 부산','부산광역시 해운대구 해운대해변로 292, 엘시티 랜드마크타워',210000,'deluxe','https://pix8.agoda.net/hotelImages/16933389/-1/6d860134a155ab6cc0c72a6b90e80952.jpg?ce=0&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',2,'AVAILABLE'),

-- 파크 하얏트 부산(accom_id=6 방 종류 1개 방 3개씩)
    -- 8월 1일: 모든 방 AVAILABLE (검색 가능)
    (71,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',3,'AVAILABLE'),
    (72,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',3,'AVAILABLE'),
    (73,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-01 15:00:00', '2025-08-02 11:00:00',3,'AVAILABLE'),
    -- 8월 2일: 모든 방 AVAILABLE (검색 가능)
    (74,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',3,'AVAILABLE'),
    (75,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',3,'AVAILABLE'),
    (76,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-02 15:00:00', '2025-08-03 11:00:00',3,'AVAILABLE'),
    -- 8월 3일: 모든 방 AVAILABLE (검색 가능)
    (77,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',3,'AVAILABLE'),
    (78,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',3,'AVAILABLE'),
    (79,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-03 15:00:00', '2025-08-04 11:00:00',3,'AVAILABLE'),
    -- 8월 4일: 모든 방 AVAILABLE (검색 가능)
    (80,6,4,4,1,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',3,'CONFIRMED'),
    (81,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',3,'AVAILABLE'),
    (82,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-04 15:00:00', '2025-08-05 11:00:00',3,'AVAILABLE'),
    -- 8월 5일: 모든 방 AVAILABLE (검색 가능)
    (83,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',3,'AVAILABLE'),
    (84,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',3,'AVAILABLE'),
    (85,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-05 15:00:00', '2025-08-06 11:00:00',3,'AVAILABLE'),
    -- 8월 6일: 모든 방 AVAILABLE (검색 가능)
    (86,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',3,'AVAILABLE'),
    (87,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',3,'AVAILABLE'),
    (88,6,NULL,NULL,NULL,'파크 하얏트 부산','부산광역시 해운대구 마린시티1로 51',400000,'deluxe','https://pix8.agoda.net/hotelImages/411082/3261310/adb8d89ee491f510b41d30d309720679.jpeg?ce=0&s=1024x','2025-08-06 15:00:00', '2025-08-07 11:00:00',3,'AVAILABLE');

-- 식당 정보 테스트용 데이터 (가짜 firebase 경로)
INSERT INTO RESTAURANT_INFO (
    rest_name, address, category, rest_image_url,
    phone, description, latitude, longitude, menu_url
) VALUES
-- 한식 (rest_id: 1 ~ 3)
('해운대 곰장어집', '부산광역시 해운대구 중동2로 10', 'korean',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/korean1.jpg?alt=media',
 '051-111-2222', '불맛 가득한 곰장어 전문점', 35.163, 129.163, 'https://example.com/korean1'),

('부산 밀면집', '부산광역시 동래구 충렬대로 237', 'korean',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/korean2.jpg?alt=media',
 '051-333-4444', '시원한 육수의 전통 밀면집', 35.205, 129.086, 'https://example.com/korean2'),

('돼지국밥천국', '부산광역시 부산진구 중앙대로 708', 'korean',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/korean3.jpg?alt=media',
 '051-555-6666', '진한 육수의 돼지국밥 명가', 35.152, 129.060, 'https://example.com/korean3'),

-- 일식 (4 ~ 6)
('스시하루 부산점', '부산광역시 수영구 수영로 570', 'japanese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/japanese1.jpg?alt=media',
 '051-777-8888', '정통 오마카세 전문점', 35.160, 129.115, 'https://example.com/japanese1'),

('이자카야 코이', '부산광역시 남구 용소로 45', 'japanese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/japanese2.jpg?alt=media',
 '051-222-3333', '감성 가득한 이자카야', 35.128, 129.093, 'https://example.com/japanese2'),

('돈카츠연구소', '부산광역시 해운대구 해운대로 569', 'japanese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/japanese3.jpg?alt=media',
 '051-444-5555', '두툼한 수제 돈카츠', 35.162, 129.163, 'https://example.com/japanese3'),

-- 중식 (7 ~ 9)
('팔선생 중화요리', '부산광역시 부산진구 동천로 92', 'chinese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/chinese1.jpg?alt=media',
 '051-123-1234', '짜장면·짬뽕 명가', 35.160, 129.065, 'https://example.com/chinese1'),

('홍콩반점41 부산대점', '부산광역시 금정구 장전온천천로 66', 'chinese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/chinese2.jpg?alt=media',
 '051-321-4321', '프랜차이즈 중식당', 35.230, 129.089, 'https://example.com/chinese2'),

('루이하오 중국요리', '부산광역시 연제구 중앙대로 1156', 'chinese',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/chinese3.jpg?alt=media',
 '051-444-2222', '정통 중식당', 35.181, 129.081, 'https://example.com/chinese3'),

-- 양식 (10 ~ 12)
('부산 파스타하우스', '부산광역시 수영구 광안해변로 193', 'western',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/western1.jpg?alt=media',
 '051-123-4567', '오션뷰 감성 파스타 맛집', 35.153, 129.118, 'https://example.com/western1'),

('스테이크 팩토리', '부산광역시 남구 분포로 145', 'western',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/western2.jpg?alt=media',
 '051-999-8888', '두툼한 스테이크 전문점', 35.127, 129.100, 'https://example.com/western2'),

('마리나 피자', '부산광역시 해운대구 우동 1418-2', 'western',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/western3.jpg?alt=media',
 '051-555-7777', '화덕 피자 전문점', 35.164, 129.163, 'https://example.com/western3'),

-- 기타 (13 ~ 15)
('비건그린 키친', '부산광역시 동구 중앙대로 248', 'etc',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/etc1.jpg?alt=media',
 '051-000-1111', '채식주의자를 위한 건강식당', 35.137, 129.059, 'https://example.com/etc1'),

('버블티&카페', '부산광역시 해운대구 좌동로 63', 'etc',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/etc2.jpg?alt=media',
 '051-321-8765', '음료와 디저트 전문 카페', 35.167, 129.176, 'https://example.com/etc2'),

('이색분식연구소', '부산광역시 중구 광복로 12', 'etc',
 'https://firebasestorage.googleapis.com/v0/b/test-app.appspot.com/o/etc3.jpg?alt=media',
 '051-888-1111', '퓨전 분식 전문점', 35.101, 129.033, 'https://example.com/etc3');

-- 식당 시간 테스트용 데이터
-- 식당별 예약 시간 슬롯 생성 (rest_id: 1 ~ 15, 시간: 11시 ~ 19시, max_capacity: 5)
INSERT INTO REST_TIME_SLOT (rest_id, res_time, max_capacity) VALUES
-- 해운대 곰장어집 (1) (1 ~ 9)
(1, '11:00', 5), (1, '12:00', 5), (1, '13:00', 5),
(1, '14:00', 5), (1, '15:00', 5), (1, '16:00', 5),
(1, '17:00', 5), (1, '18:00', 5), (1, '19:00', 5),

-- 부산 밀면집 (2) (10 ~ 18)
(2, '11:00', 5), (2, '12:00', 5), (2, '13:00', 5),
(2, '14:00', 5), (2, '15:00', 5), (2, '16:00', 5),
(2, '17:00', 5), (2, '18:00', 5), (2, '19:00', 5),

-- 돼지국밥천국 (3) (19 ~ 27)
(3, '11:00', 5), (3, '12:00', 5), (3, '13:00', 5),
(3, '14:00', 5), (3, '15:00', 5), (3, '16:00', 5),
(3, '17:00', 5), (3, '18:00', 5), (3, '19:00', 5),

-- 스시하루 부산점 (4) (28 ~ 36)
(4, '11:00', 5), (4, '12:00', 5), (4, '13:00', 5),
(4, '14:00', 5), (4, '15:00', 5), (4, '16:00', 5),
(4, '17:00', 5), (4, '18:00', 5), (4, '19:00', 5),

-- 이자카야 코지 (5) (37 ~ 45)
(5, '11:00', 5), (5, '12:00', 5), (5, '13:00', 5),
(5, '14:00', 5), (5, '15:00', 5), (5, '16:00', 5),
(5, '17:00', 5), (5, '18:00', 5), (5, '19:00', 5),

-- 돈카츠연구소 (6) (46 ~ 54)
(6, '11:00', 5), (6, '12:00', 5), (6, '13:00', 5),
(6, '14:00', 5), (6, '15:00', 5), (6, '16:00', 5),
(6, '17:00', 5), (6, '18:00', 5), (6, '19:00', 5),

-- 팔선생 중화요리 (7) (55 ~ 63)
(7, '11:00', 5), (7, '12:00', 5), (7, '13:00', 5),
(7, '14:00', 5), (7, '15:00', 5), (7, '16:00', 5),
(7, '17:00', 5), (7, '18:00', 5), (7, '19:00', 5),

-- 홍콩반점41 부산대점 (8) (64 ~ 72)
(8, '11:00', 5), (8, '12:00', 5), (8, '13:00', 5),
(8, '14:00', 5), (8, '15:00', 5), (8, '16:00', 5),
(8, '17:00', 5), (8, '18:00', 5), (8, '19:00', 5),

-- 루이하오 중국요리 (9) (73 ~ 81)
(9, '11:00', 5), (9, '12:00', 5), (9, '13:00', 5),
(9, '14:00', 5), (9, '15:00', 5), (9, '16:00', 5),
(9, '17:00', 5), (9, '18:00', 5), (9, '19:00', 5),

-- 부산 파스타하우스 (10) (82 ~ 90)
(10, '11:00', 5), (10, '12:00', 5), (10, '13:00', 5),
(10, '14:00', 5), (10, '15:00', 5), (10, '16:00', 5),
(10, '17:00', 5), (10, '18:00', 5), (10, '19:00', 5),

-- 스테이크 팩토리 (11) (91 ~ 99)
(11, '11:00', 5), (11, '12:00', 5), (11, '13:00', 5),
(11, '14:00', 5), (11, '15:00', 5), (11, '16:00', 5),
(11, '17:00', 5), (11, '18:00', 5), (11, '19:00', 5),

-- 마리나 피자 (12) (100 ~ 108)
(12, '11:00', 5), (12, '12:00', 5), (12, '13:00', 5),
(12, '14:00', 5), (12, '15:00', 5), (12, '16:00', 5),
(12, '17:00', 5), (12, '18:00', 5), (12, '19:00', 5),

-- 비건그린 키친 (13) (109 ~ 117)
(13, '11:00', 5), (13, '12:00', 5), (13, '13:00', 5),
(13, '14:00', 5), (13, '15:00', 5), (13, '16:00', 5),
(13, '17:00', 5), (13, '18:00', 5), (13, '19:00', 5),

-- 버블티&카페 (14) (118 ~ 126)
(14, '11:00', 5), (14, '12:00', 5), (14, '13:00', 5),
(14, '14:00', 5), (14, '15:00', 5), (14, '16:00', 5),
(14, '17:00', 5), (14, '18:00', 5), (14, '19:00', 5),

-- 이색분식연구소 (15) (127 ~ 135)
(15, '11:00', 5), (15, '12:00', 5), (15, '13:00', 5),
(15, '14:00', 5), (15, '15:00', 5), (15, '16:00', 5),
(15, '17:00', 5), (15, '18:00', 5), (15, '19:00', 5);

-- 비용 테스트용 데이터
INSERT INTO EXPENSE (trip_id, member_id, expense_name, expense_date, amount, location, settlement_completed)
VALUES (2, 2, '교통비', '2025-08-05 17:10:00' ,149400, 'BUSAN', false),
       (2, 2, '숙박 비용', '2025-08-06 19:10:00' ,193000, 'BUSAN', false),
       (2, 3, '돼지 국밥', '2025-08-06 19:10:00' ,45000, 'BUSAN', false),
       (2, 4, '부산 밀면', '2025-08-06 19:10:00' ,30000, 'BUSAN', false),
       (2, 2, '부산 꼼장어', '2025-08-06 19:10:00' ,50000, 'BUSAN', false),
       (2, 2, '숙박 비용', '2025-08-06 19:10:00' ,210000, 'BUSAN', false),
       (2, 3, '이재모 피자', '2025-08-06 19:10:00' ,40000, 'BUSAN', false),
       (2, 4, '버블티&카페', '2025-08-06 19:10:00' ,18000, 'BUSAN', false),
       (2, 2, '교통비', '2025-08-05 17:10:00' ,149400, 'BUSAN', false);

INSERT INTO SETTLEMENT_NOTES (expense_id, trip_id, member_id, share_amount, is_payed, received)
VALUES
-- (2, 2, '교통비', 149400, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(1, 2, 2, 49800, true, false),
(1, 2, 3, 49800, false, true),
(1, 2, 4, 49800, false, true),
-- (2, 2, '숙박 비용', 193000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(2, 2, 2, 38600, true, false),
(2, 2, 3, 38600, false, true),
(2, 2, 4, 38600, false, true),
-- (2, 3, '돼지 국밥', 45000, 'BUSAN', false)에 대한 정산 (닝닝이 계산)
(3, 2, 2, 15000, false, true),
(3, 2, 3, 15000, true, false),
(3, 2, 4, 15000, false, true),
-- (2, 2, '부산 밀면', 30000, 'BUSAN', false)에 대한 정산 (지젤이 계산)
(4, 2, 2, 10000, false, true),
(4, 2, 3, 10000, false, true),
(4, 2, 4, 10000, true, false),
-- (2, 2, '부산 꼼장어', 50000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(5, 2, 2, 16667, true, false),
(5, 2, 3, 16666, false, true),
(5, 2, 4, 16666, false, true),
-- (2, 2, '숙박 비용', 210000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(6, 2, 2, 70000, true, false),
(6, 2, 3, 70000, false, true),
(6, 2, 4, 70000, false, true),
-- (2, 3, '이재모 피자', 40000, 'BUSAN', false)에 대한 정산 (닝닝이 계산)
(7, 2, 2, 13334, false, true),
(7, 2, 3, 13333, true, false),
(7, 2, 4, 13333, false, true),
-- (2, 2, '버블티&카페', 18000, 'BUSAN', false)에 대한 정산 (지젤이 계산)
(8, 2, 2, 6000, false, true),
(8, 2, 3, 6000, false, true),
(8, 2, 4, 6000, true, false),
-- (2, 2, '교통비', 149400, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(9, 2, 2, 49800, true, false),
(9, 2, 3, 49800, false, true),
(9, 2, 4, 49800, false, true);

INSERT INTO TRIP_RECORDS (trip_id, member_id, title, record_date, content)
VALUES (1, 1, '부산 도착~', '2025-08-01', '내일 이재모 피자 먹어야지 ㅎㅎ 숙소도 너무 좋다'),
       (1, 1, '이재모 피자', '2025-08-02', '진짜 맛있음. 다음엔 다른 메뉴 먹어봐야지'),
       (1, 1, '남포동 투어', '2025-08-02', null),
       (1, 1, '롯데호텔 조식', '2025-08-03', '짱 맛있음'),
       (1, 1, '해운대', '2025-08-03', '더운데 경치가 너무 좋았다~~'),
       (1, 1, '부산 마지막날 ㅜㅜ', '2025-08-04', '아쉽다. 다음에 또 와야지');

INSERT INTO TRIP_RECORD_IMAGES (record_id, image_url)
VALUES (1, '2b1d7834-3f92-4fb8-8e39-dac2c952c6f4.webp'),
       (1, '1482b39b-981d-43d9-9747-446f0c08c545.jpg'),
       (2, '73eed0e4-cfeb-4fad-b890-45a95affb559.webp'),
       (2, '6078349e-db19-4a53-9d82-f9b2e81cbee3.png'),
       (3, '773215b6-3a11-4dc5-8050-234d3aaf7b78.webp'),
       (4, 'b290a04d-d05c-4975-9d52-cd56138bdaf5.jpg'),
       (5, '7fb93cf9-02d4-4678-8a6b-d30371dfcf75.jpg');

-- 숙박 테스트 데이터셋 추가
# INSERT INTO RESERVATION (trip_day_id, res_kind)
# VALUES
# -- trip_day_id: 1, 2, 3, 4 => 카리나 부산 여행
# (1,'ACCOMMODATION'), -- 숙박
# (2,'ACCOMMODATION'), -- 숙박
# (3,'ACCOMMODATION'), -- 숙박
# (4,'ACCOMMODATION'), -- 숙박
# -- trip_day_id : 5, 6, 7 => 윈닝젤 부산 여행
# (5,'ACCOMMODATION'), -- 숙박
# (6,'ACCOMMODATION'), -- 숙박
# (7,'ACCOMMODATION'); -- 숙박

-- 교통 테스트 데이터셋 추가