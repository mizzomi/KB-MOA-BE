-- UTF-8 인코딩 설정
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;

-- 초기화
SET FOREIGN_KEY_CHECKS = 0;

-- DROP TABLES (FK 자식부터 → 부모순)
DROP TABLE IF EXISTS trip_record_images;
DROP TABLE IF EXISTS trip_records;
DROP TABLE IF EXISTS settlement_notes;
DROP TABLE IF EXISTS expense;
DROP TABLE IF EXISTS rest_time_slot;
DROP TABLE IF EXISTS rest_res;
DROP TABLE IF EXISTS restaurant_info;
DROP TABLE IF EXISTS tran_res;
DROP TABLE IF EXISTS transport_info;
DROP TABLE IF EXISTS accom_res;
DROP TABLE IF EXISTS room_info;
DROP TABLE IF EXISTS accommodation_info;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS trip_day;
DROP TABLE IF EXISTS trip_member;
DROP TABLE IF EXISTS trip_location;
DROP TABLE IF EXISTS trip;
DROP TABLE IF EXISTS payment_record;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS driver_license;
DROP TABLE IF EXISTS id_card;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS owner;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS tbl_security_audit_log;

-- ========================================================================================
-- 회원 테이블
-- ========================================================================================
CREATE TABLE member
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
CREATE TABLE owner
(
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

    PRIMARY KEY (business_id, business_kind),

    CONSTRAINT fk_owner_member
        FOREIGN KEY (member_id)
            REFERENCES member(member_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- Security Audit Log 테이블
-- ========================================================================================
CREATE TABLE IF NOT EXISTS tbl_security_audit_log
(
    AUDIT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    EVENT_TYPE VARCHAR(50) NOT NULL COMMENT '이벤트 타입',
    MEMBER_ID BIGINT COMMENT '회원 ID',
    USERNAME VARCHAR(100) COMMENT '사용자명',
    IP_ADDRESS VARCHAR(45) NOT NULL COMMENT 'IP 주소',
    USER_AGENT TEXT COMMENT '브라우저 정보',
    REQUEST_METHOD VARCHAR(10) COMMENT 'HTTP 메소드',
    REQUEST_URI VARCHAR(255) COMMENT '요청 URI',
    EVENT_DETAIL TEXT COMMENT '상세 정보',
    SUCCESS_YN CHAR(1) DEFAULT 'N' COMMENT '성공 여부',
    ERROR_MESSAGE TEXT COMMENT '에러 메시지',
    SESSION_ID VARCHAR(100) COMMENT '세션 ID',
    ACCESS_TOKEN_ID VARCHAR(100) COMMENT 'Access Token ID',
    REFRESH_TOKEN_ID VARCHAR(100) COMMENT 'Refresh Token ID',
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',

    INDEX IDX_MEMBER_ID (MEMBER_ID),
    INDEX IDX_EVENT_TYPE (EVENT_TYPE),
    INDEX IDX_CREATED_AT (CREATED_AT),
    INDEX IDX_IP_ADDRESS (IP_ADDRESS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='보안 감사 로그';


-- ========================================================================================
-- 주민등록증 테이블
-- ========================================================================================
CREATE TABLE id_card
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
    CONSTRAINT fk_member_to_id_card_1
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
);

-- ========================================================================================
-- 운전면허증 테이블
-- ========================================================================================
CREATE TABLE driver_license
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
    FOREIGN KEY (member_id) REFERENCES member (member_id)
);

-- ========================================================================================
-- 계좌 테이블
-- ========================================================================================
CREATE TABLE account
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
    FOREIGN KEY (member_id) REFERENCES member (member_id)
);

-- ========================================================================================
-- 여행 테이블
-- ========================================================================================
CREATE TABLE trip
(
    trip_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT                              NOT NULL,
    trip_name     VARCHAR(255),
    trip_location ENUM ('BUSAN', 'GANGNEUNG', 'JEJU', 'SEOUL') NOT NULL,
    start_date    DATE                                NOT NULL,
    end_date      DATE                                NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (member_id)
);

-- ========================================================================================
-- 여행 위치 테이블
-- ========================================================================================
CREATE TABLE trip_location
(
    location_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    location_name ENUM ('BUSAN', 'GANGNEUNG', 'JEJU', 'SEOUL') NOT NULL,
    latitude      DECIMAL(10, 8),
    longitude     DECIMAL(11, 8),
    address       VARCHAR(200)
);

-- ========================================================================================
-- 여행 멤버 테이블
-- ========================================================================================
CREATE TABLE trip_member
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id   BIGINT                  NOT NULL,
    member_id BIGINT                  NOT NULL,

    role      ENUM ('HOST', 'MEMBER') NOT NULL,
    joined_at DATETIME                NOT NULL,

    FOREIGN KEY (trip_id) REFERENCES trip (trip_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id),

    UNIQUE KEY unique_trip_member (trip_id, member_id)
);

-- ========================================================================================
-- 여행 날짜 테이블
-- ========================================================================================
CREATE TABLE trip_day
(
    trip_day_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trip_id     BIGINT NOT NULL,
    day         DATE   NOT NULL,

    CONSTRAINT fk_trip_day_trip_id
        FOREIGN KEY (trip_id)
            REFERENCES trip (trip_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 결제 내역 테이블
-- ========================================================================================
CREATE TABLE payment_record
(
    record_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT         NOT NULL,
    member_id        BIGINT         NOT NULL,
    trip_day_id      BIGINT         NULL,
    payment_name     VARCHAR(100)   NOT NULL,
    payment_price    DECIMAL(15, 2) NOT NULL,
    payment_date     DATETIME       NOT NULL,
    payment_location VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES account (account_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id),
    FOREIGN KEY (trip_day_id) REFERENCES trip_day (trip_day_id)
);

-- ========================================================================================
-- 예약 테이블
-- ========================================================================================
CREATE TABLE reservation
(
    reservation_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trip_day_id    BIGINT NOT NULL,
    res_kind       ENUM ('TRANSPORT', 'ACCOMMODATION', 'RESTAURANT'),

    CONSTRAINT fk_reservation_trip_day_id
        FOREIGN KEY (trip_day_id)
            REFERENCES trip_day (trip_day_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 숙박 정보 테이블
-- ========================================================================================
CREATE TABLE accommodation_info
(
    accom_id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hotel_name      VARCHAR(255) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    location        ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,
    latitude        DECIMAL(10, 7),
    longitude       DECIMAL(10, 7),
    description     TEXT,
    hotel_image_url VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========================================================================================
-- 숙박 예약 테이블
-- ========================================================================================
CREATE TABLE accom_res
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

    FOREIGN KEY (accom_id) REFERENCES accommodation_info (accom_id),
    FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES trip_day (trip_day_id)
);

-- ========================================================================================
-- 교통 정보 테이블
-- ========================================================================================
CREATE TABLE transport_info
(
    transport_id   BIGINT                                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
    departure_id   VARCHAR(30)                                         NOT NULL,
    arrival_id     VARCHAR(30)                                         NOT NULL,
    departure_name VARCHAR(100),
    arrival_name   VARCHAR(100),
    departure_time DATETIME                                            NOT NULL,
    arrival_time   DATETIME                                            NOT NULL,
    train_type     ENUM ('ktx', 'ktx-sancheon', 'ktx-eum')             NOT NULL,
    train_no       VARCHAR(30)                                         NOT NULL,
    seat_type      ENUM ('general', 'first_class', 'silent', 'family') NOT NULL,
    seat_total     INT                                                 NOT NULL,
    seat_remain    INT                                                 NOT NULL,
    price          DECIMAL(15, 2)                                      NOT NULL,
    is_visible     BOOLEAN                                             NOT NULL
);

-- ========================================================================================
-- 교통 예약 테이블
-- ========================================================================================
CREATE TABLE tran_res
(
    tran_res_id    BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transport_id   BIGINT   NOT NULL,
    reservation_id BIGINT   NULL,
    trip_day_id    BIGINT   NULL,
    departure      VARCHAR(30)   NOT NULL,
    arrival        VARCHAR(30)   NOT NULL,
    seat_room_no   INT        NOT NULL,
    seat_number    VARCHAR(10)   NOT NULL,
    seat_type      ENUM ('general', 'first_class', 'silent', 'family') NOT NULL,
    booked_at      DATETIME   NULL,
    price          DECIMAL(15, 2)   NOT NULL,
    version        INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status         ENUM('AVAILABLE', 'PENDING', 'CONFIRMED') NOT NULL DEFAULT 'AVAILABLE',

    FOREIGN KEY (transport_id) REFERENCES transport_info (transport_id) ON DELETE CASCADE,

    CONSTRAINT fk_tran_res_reservation_id
        FOREIGN KEY (reservation_id)
            REFERENCES reservation (reservation_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_tran_res_trip_day_id
        FOREIGN KEY (trip_day_id)
            REFERENCES trip_day (trip_day_id)
            ON DELETE CASCADE
);

-- ========================================================================================
-- 식당 정보 테이블
-- ========================================================================================
CREATE TABLE restaurant_info
(
    rest_id        BIGINT                                                                        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rest_name      VARCHAR(255)                                                                  NOT NULL,
    address        VARCHAR(255)                                                                  NOT NULL,
    category       ENUM ('korean', 'chinese', 'japanese', 'western', 'etc')                      NOT NULL,
    rest_image_url VARCHAR(500)                                                                  NOT NULL,
    phone          VARCHAR(20)                                                                   NULL,
    description    TEXT                                                                          NULL,
    latitude       DECIMAL(10, 7)                                                                NULL,
    longitude      DECIMAL(10, 7)                                                                NULL,
    menu_url       VARCHAR(500)                                                                  NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========================================================================================
-- 식당 시간 테이블
-- ========================================================================================
CREATE TABLE rest_time_slot
(
    rest_time_id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rest_id        BIGINT       NOT NULL,
    res_time       VARCHAR(10)  NOT NULL,
    max_capacity   INT          NOT NULL DEFAULT 10,

    FOREIGN KEY (rest_id) REFERENCES restaurant_info (rest_id)
);

-- ========================================================================================
-- 식당 예약 테이블
-- ========================================================================================
CREATE TABLE rest_res
(
    rest_res_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rest_id        BIGINT       NOT NULL,
    reservation_id BIGINT       NOT NULL,
    trip_day_id    BIGINT       NOT NULL,
    res_num        INT          NOT NULL,
    rest_time_id   BIGINT       NOT NULL,
    status         ENUM('reserved', 'checked_in', 'completed') DEFAULT 'reserved' NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (rest_id) REFERENCES restaurant_info (rest_id),
    FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES trip_day (trip_day_id),
    FOREIGN KEY (rest_time_id) REFERENCES rest_time_slot (rest_time_id)
);

-- ========================================================================================
-- 비용 테이블
-- ========================================================================================
CREATE TABLE expense
(
    expense_id           BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trip_id              BIGINT                              NOT NULL,
    member_id            BIGINT                              NOT NULL,
    expense_name         VARCHAR(100)                        NOT NULL,
    expense_date         DATETIME                            NOT NULL,
    amount               DECIMAL(15, 2)                      NOT NULL,
    location             ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,
    settlement_completed BOOLEAN                             NOT NULL,
    created_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (trip_id) REFERENCES trip (trip_id)
        ON DELETE CASCADE
);

-- ========================================================================================
-- 알림 테이블
-- ========================================================================================
CREATE TABLE notification
(
    notification_id   BIGINT                                NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id         BIGINT                                NOT NULL,
    trip_id           BIGINT       NULL,
    expense_id        BIGINT       NULL,
    notification_type ENUM ('TRIP', 'SETTLE') NOT NULL,
    sender_name       VARCHAR(100) NOT NULL,
    trip_name         VARCHAR(255) NOT NULL,
    title             VARCHAR(100) NULL,
    content           TEXT         NULL,
    is_read           BOOLEAN                               NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP                             NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (member_id) REFERENCES member (member_id),
    FOREIGN KEY (trip_id) REFERENCES trip (trip_id),
    FOREIGN KEY (expense_id) REFERENCES expense (expense_id)
);

-- ========================================================================================
-- 정산 내역 테이블
-- ========================================================================================
CREATE TABLE settlement_notes
(
    settlement_id BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    expense_id    BIGINT         NOT NULL,
    trip_id       BIGINT         NOT NULL,
    member_id     VARCHAR(36)    NOT NULL,
    share_amount  DECIMAL(15, 2) NOT NULL,
    is_payed      BOOLEAN        NOT NULL,
    received      BOOLEAN        NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (expense_id) REFERENCES expense (expense_id)
        ON DELETE CASCADE,

    FOREIGN KEY (trip_id) REFERENCES trip (trip_id)
        ON DELETE CASCADE
);

-- ========================================================================================
-- 여행 기록 테이블
-- ========================================================================================
CREATE TABLE trip_records
(
    record_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id     BIGINT       NOT NULL,
    member_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    record_date DATE         NOT NULL,
    content     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_record_to_member FOREIGN KEY (member_id) REFERENCES member (member_id),

    INDEX idx_trip_id_record_date (trip_id, record_date)
);

-- ========================================================================================
-- 여행 기록 사진 테이블
-- ========================================================================================
CREATE TABLE trip_record_images
(
    image_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id   BIGINT       NOT NULL,
    image_url   VARCHAR(1024) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_trip_record_image_record_id_v2
        FOREIGN KEY (record_id)
            REFERENCES trip_records (record_id)
            ON DELETE CASCADE
);

-- 회원 테스트용 데이터
INSERT INTO member (member_id, member_type, email, password, name, fcm_token, id_card_number)
VALUES (1, 'ROLE_USER', 'karina@test.com', '1234', '카리나', 'asdf1234', '0004114000001'),
       (2, 'ROLE_USER', 'winter@test.com', '1234', '윈터', 'qwer1234', '0101014000002'),
       (3, 'ROLE_USER', 'giselle@test.com', '1234', '지젤', 'asdf5678', '0010304000003'),
       (4, 'ROLE_USER', 'ningning@test.com', '1234', '닝닝', 'qwer5678', '0210234000002');


-- 1) MEMBER 테이블에 사업자 계정 추가
INSERT INTO member (member_id, member_type, email, password, name, fcm_token, id_card_number)
VALUES
      (5, 'ROLE_OWNER', '123-45-67890',    '1234',  '교통사업자',       NULL, 'OWN0000001'),
      (6, 'ROLE_OWNER', '987-65-43210',    '1234',  '숙박사업자',       NULL, 'OWN0000002'),
      (7, 'ROLE_OWNER', '456-78-90123-1',    '1234',  '해운대곰장어사업자', NULL, 'OWN0000003'),
      (8, 'ROLE_OWNER', '456-78-90123-2',    '1234',  '코이이자카야사업자', NULL, 'OWN0000004'),
      (9, 'ROLE_OWNER', '456-78-90123-3',    '1234',  '팔선생중화요리사업자',NULL,'OWN0000005'),
      (10, 'ROLE_OWNER', '789-23-45678-4',    '1234',  '파스타하우스사업자',  NULL, 'OWN0000006'),
      (11, 'ROLE_OWNER', '456-78-90123-5',    '1234',  '이색분식연구소사업자',NULL,'OWN0000007');


-- 사장님 테스트용 데이터
-- 2) OWNER 테이블에 business ↔ member 연결
INSERT INTO owner (business_id, business_kind, member_id)
VALUES
      ( 1, 'TRANSPORT',    5),
      ( 1, 'ACCOMMODATION', 6),
      ( 1, 'RESTAURANT',   7),  -- 해운대 곰장어집
      ( 5, 'RESTAURANT',   8),  -- 코이 이자카야
      ( 7, 'RESTAURANT',   9),  -- 팔선생 중화요리
      (10, 'RESTAURANT',  10),  -- 부산 파스타하우스
      (15, 'RESTAURANT',  11);   -- 이색분식연구소


-- 주민등록증 테스트용 데이터
INSERT INTO id_card (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (1, '0004114000001', '카리나', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/karina.jfif');

INSERT INTO id_card (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (2, '0101014000002', '윈터', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/winter.jfif');

INSERT INTO id_card (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (3, '0010304000003', '지젤', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/giselle.jfif');

INSERT INTO id_card (member_id, id_card_number, name, issued_date, address, image_url)
VALUES (4, '0210234000002', '닝닝', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', 'IDphoto/ningning.jfif');


-- 운전면허증 테스트용 데이터
-- 카리나 (경남 양산 → 울산남부경찰서)
INSERT INTO driver_license (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (1, '0004114000001', '201234567810', '1종 보통',
        '2020-06-15', '2030-06-15', '울산남부경찰서', 'IDphoto/karina.jfif');

-- 윈터 (부산 해운대 → 부산해운대경찰서)
INSERT INTO driver_license (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (2, '0101014000002', '211198765421', '2종 소형',
        '2021-03-10', '2031-03-10', '부산해운대경찰서', 'IDphoto/winter.jfif');

-- 지젤 (도쿄 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO driver_license (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (3, '0010304000003', '221011223309', '2종 보통',
        '2022-07-22', '2032-07-22', '서울성동경찰서', 'IDphoto/giselle.jfif');

-- 닝닝 (하얼빈 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO driver_license (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency, image_url)
VALUES (4, '0210234000002', '230944556633', '원동기장치자전거',
        '2023-01-05', '2033-01-05', '서울성동경찰서', 'IDphoto/ningning.jfif');


-- 계좌 테스트용 데이터
-- 카리나
INSERT INTO account (member_id, name, account_number, account_password, bank_name, balance)
VALUES (1, '카리나', '1234567890001', '1111', 'KB', 10000000.00);

-- 윈터
INSERT INTO account (member_id, name, account_number, account_password, bank_name, balance)
VALUES (2, '윈터', '1234567890002', '2222', 'KB', 1000000.00);

-- 지젤
INSERT INTO account (member_id, name, account_number, account_password, bank_name, balance)
VALUES (3, '지젤', '1234567890003', '3333', 'KB', 100000.00);

-- 닝닝
INSERT INTO account (member_id, name, account_number, account_password, bank_name, balance)
VALUES (4, '닝닝', '1234567890004', '4444', 'KB', 1000000.00);


-- 결제 내역 테스트용 데이터
-- 카리나 (member_id=1)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (1, 1, '편의점 간식', 4500.00, '2025-08-01 09:10:00', 'BUSAN'),
       (1, 1, '부산 파스타하우스', 21000.00, '2025-08-03 12:43:25', 'BUSAN'),
       (1, 1, '카페 커피', 6500.00, '2025-08-05 15:40:00', 'BUSAN');

-- 윈터 (member_id=2)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (2, 2, '주유소 결제', 54000.00, '2025-08-03 10:20:00', 'BUSAN'),
       (2, 2, '숙박 비용', 193000.00, '2025-08-03 12:00:00', 'BUSAN'),
       (2, 2, '교통비', 105000.00, '2025-08-04 13:30:00', 'BUSAN'),
       (2, 2, '길거리 간식', 9999.00, '2025-08-04 17:30:00', 'BUSAN'),
       (2, 2, '편의점 물품', 12000.00, '2025-08-05 22:10:00', 'BUSAN');

-- 지젤 (member_id=3)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (3, 3, '팔선생 중화요리', 61000.00, '2025-08-04 12:56:43', 'BUSAN'),
       (3, 3, '이색분식연구소', 10000.00, '2025-08-04 19:44:26', 'BUSAN'),
       (3, 3, '기념품 구매', 22000.00, '2025-08-04 11:15:00', 'BUSAN'),
       (3, 3, '카페 디저트', 8500.00, '2025-08-04 15:45:00', 'BUSAN');

-- 닝닝 (member_id=4)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (4, 4, '이자카야 코이', 24300.00, '2025-08-03 18:35:47', 'BUSAN'),
       (4, 4, '편의점 간식', 5500.00, '2025-08-04 12:20:00', 'BUSAN'),
       (4, 4, '기념품', 17000.00, '2025-08-05 16:30:00', 'BUSAN');


-- 여행 테스트용 데이터 (카리나 1인여행 8/1 ~ 8/4)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '혼자 부산 여행', 'BUSAN', '2025-08-01', '2025-08-04');

-- 여행 테스트용 데이터 (윈터, 지젤, 닝닝 3인여행 8/3 ~ 8/5)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '셋이서 부산 여행', 'BUSAN', '2025-08-03', '2025-08-05');

-- 여행 데이터 추가
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date) VALUES
-- 3월: BUSAN
(1, '봄바람 휘날리는 부산', 'BUSAN', '2025-03-10', '2025-03-12'),

-- 4월: GANGNEUNG
(2, '벚꽃 피는 강릉 여행', 'GANGNEUNG', '2025-04-05', '2025-04-07'),

-- 5월: JEJU
(3, '초여름 제주 한 바퀴', 'JEJU', '2025-05-15', '2025-05-18'),

-- 6월: SEOUL
(4, '서울 핫플 투어', 'SEOUL', '2025-06-02', '2025-06-05'),

-- 7월: BUSAN
(1, '여름맞이 해운대 여행', 'BUSAN', '2025-07-10', '2025-07-13'),

-- 8월: JEJU
(2, '한여름 제주 폭염 탈출', 'JEJU', '2025-08-20', '2025-08-24'),

-- 9월: GANGNEUNG
(3, '추석 연휴 강릉 힐링여행', 'GANGNEUNG', '2025-09-10', '2025-09-12'),

-- 10월: BUSAN
(4, '부산 불꽃 축제 여행', 'BUSAN', '2025-10-04', '2025-10-06'),

-- 10월 말: SEOUL
(1, '서울 할로윈 나들이', 'SEOUL', '2025-10-28', '2025-10-30'),

-- 11월: JEJU
(2, '늦가을 제주 단풍 여행', 'JEJU', '2025-11-10', '2025-11-13'),

-- 12월 초: SEOUL
(3, '서울 크리스마스 마켓 투어', 'SEOUL', '2025-12-02', '2025-12-04'),

-- 12월 중순: JEJU
(4, '연말 제주 감귤 체험', 'JEJU', '2025-12-15', '2025-12-17'),

-- 12월 말: JEJU
(1, '2025 마무리 제주여행', 'JEJU', '2025-12-27', '2025-12-30');


-- 여행 위치 테스트용 데이터
INSERT INTO trip_location (location_name, latitude, longitude, address)
VALUES ('BUSAN', 35.179554, 129.075642, '부산광역시 중구 중앙대로 100'),
       ('GANGNEUNG', 37.751853, 128.876057, '강원특별자치도 강릉시 교동광장로 100'),
       ('JEJU', 33.499621, 126.531188, '제주특별자치도 제주시 중앙로 100'),
       ('SEOUL', 37.566535, 126.977969, '서울특별시 중구 세종대로 110');


-- 여행 멤버 테스트용 데이터
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES  (1, 1, 'HOST', NOW()), -- 카리나 호스트 부산여행
        (2, 2, 'HOST', NOW()), -- 윈터 호스트 부산여행
        (2, 3, 'MEMBER', NOW()), -- 닝닝 멤버 부산여행
        (2, 4, 'MEMBER', NOW()); -- 지젤 멤버 부산여행

-- 여행 멤버 데이터 추가
INSERT INTO trip_member (trip_id, member_id, role, joined_at) VALUES
-- trip_id = 1 ~ 13
(3, 1, 'HOST', NOW()), (3, 2, 'MEMBER', NOW()), (3, 3, 'MEMBER', NOW()), (3, 4, 'MEMBER', NOW()),
(4, 2, 'HOST', NOW()), (4, 1, 'MEMBER', NOW()), (4, 3, 'MEMBER', NOW()), (4, 4, 'MEMBER', NOW()),
(5, 3, 'HOST', NOW()), (5, 1, 'MEMBER', NOW()), (5, 2, 'MEMBER', NOW()), (5, 4, 'MEMBER', NOW()),
(6, 4, 'HOST', NOW()), (6, 1, 'MEMBER', NOW()), (6, 2, 'MEMBER', NOW()), (6, 3, 'MEMBER', NOW()),
(7, 1, 'HOST', NOW()), (7, 2, 'MEMBER', NOW()), (7, 3, 'MEMBER', NOW()), (7, 4, 'MEMBER', NOW()),
(8, 2, 'HOST', NOW()), (8, 1, 'MEMBER', NOW()), (8, 3, 'MEMBER', NOW()), (8, 4, 'MEMBER', NOW()),
(9, 3, 'HOST', NOW()), (9, 1, 'MEMBER', NOW()), (9, 2, 'MEMBER', NOW()), (9, 4, 'MEMBER', NOW()),
(10, 4, 'HOST', NOW()), (10, 1, 'MEMBER', NOW()), (10, 2, 'MEMBER', NOW()), (10, 3, 'MEMBER', NOW()),
(11, 1, 'HOST', NOW()), (11, 2, 'MEMBER', NOW()), (11, 3, 'MEMBER', NOW()), (11, 4, 'MEMBER', NOW()),
(12, 2, 'HOST', NOW()), (12, 1, 'MEMBER', NOW()), (12, 3, 'MEMBER', NOW()), (12, 4, 'MEMBER', NOW()),
(13, 3, 'HOST', NOW()), (13, 1, 'MEMBER', NOW()), (13, 2, 'MEMBER', NOW()), (13, 4, 'MEMBER', NOW()),
(14, 4, 'HOST', NOW()), (14, 1, 'MEMBER', NOW()), (14, 2, 'MEMBER', NOW()), (14, 3, 'MEMBER', NOW()),
(15, 1, 'HOST', NOW()), (15, 2, 'MEMBER', NOW()), (15, 3, 'MEMBER', NOW()), (15, 4, 'MEMBER', NOW());


-- 여행 날짜 테스트용 데이터
INSERT INTO trip_day (trip_id, day)
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

-- 여행 날짜 데이터 추가
INSERT INTO trip_day (trip_id, day) VALUES
-- trip_id = 3 (2025-03-10 ~ 12)
(3, '2025-03-10'), (3, '2025-03-11'), (3, '2025-03-12'),

-- trip_id = 4 (2025-04-05 ~ 07)
(4, '2025-04-05'), (4, '2025-04-06'), (4, '2025-04-07'),

-- trip_id = 5 (2025-05-15 ~ 18)
(5, '2025-05-15'), (5, '2025-05-16'), (5, '2025-05-17'), (5, '2025-05-18'),

-- trip_id = 6 (2025-06-02 ~ 05)
(6, '2025-06-02'), (6, '2025-06-03'), (6, '2025-06-04'), (6, '2025-06-05'),

-- trip_id = 7 (2025-07-10 ~ 13)
(7, '2025-07-10'), (7, '2025-07-11'), (7, '2025-07-12'), (7, '2025-07-13'),

-- trip_id = 8 (2025-08-20 ~ 24)
(8, '2025-08-20'), (8, '2025-08-21'), (8, '2025-08-22'), (8, '2025-08-23'), (8, '2025-08-24'),

-- trip_id = 9 (2025-09-10 ~ 12)
(9, '2025-09-10'), (9, '2025-09-11'), (9, '2025-09-12'),

-- trip_id = 10 (2025-10-04 ~ 06)
(10, '2025-10-04'), (10, '2025-10-05'), (10, '2025-10-06'),

-- trip_id = 11 (2025-10-28 ~ 30)
(11, '2025-10-28'), (11, '2025-10-29'), (11, '2025-10-30'),

-- trip_id = 12 (2025-11-10 ~ 13)
(12, '2025-11-10'), (12, '2025-11-11'), (12, '2025-11-12'), (12, '2025-11-13'),

-- trip_id = 13 (2025-12-02 ~ 04)
(13, '2025-12-02'), (13, '2025-12-03'), (13, '2025-12-04'),

-- trip_id = 14 (2025-12-15 ~ 17)
(14, '2025-12-15'), (14, '2025-12-16'), (14, '2025-12-17'),

-- trip_id = 15 (2025-12-27 ~ 30)
(15, '2025-12-27'), (15, '2025-12-28'), (15, '2025-12-29'), (15, '2025-12-30');


-- 예약 테스트용 데이터
INSERT INTO reservation (trip_day_id, res_kind)
VALUES
-- trip_day_id: 1, 2, 3, 4 => 카리나 부산 여행
(1,'ACCOMMODATION'), -- 숙박
(2,'ACCOMMODATION'), -- 숙박
(3,'ACCOMMODATION'), -- 숙박
(4,'ACCOMMODATION'), -- 숙박
-- trip_day_id : 5, 6, 7 => 윈닝젤 부산 여행
(5,'ACCOMMODATION'), -- 숙박
(6,'ACCOMMODATION'), -- 숙박
-- 교통 데이터 추가
(1, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/1 서울역 -> 부산역 10:00 출발 reservationId = 7 , tripDayId = 1
(4, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/4 부산역 -> 서울역 16:00 출발 reservationId = 8, tripDayId = 4
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/3 서울역 -> 부산역 11:00 출발 (윈터, 지젤) reservationId = 9, tripDayId = 5
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/3 서울역 -> 부산역 12:00 출발 (닝닝) reservationId = 10, tripDayId = 5
(7, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/5 부산역 -> 서울역 10:00 출발 (윈터 스케줄 바쁨) reservationId = 11, tripDayId = 7
(7, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/5 부산역 -> 서울역 15:00 출발 (지젤, 닝닝 느긋) reservationId = 12, tripDayId = 7
-- 식당 데이터 추가
(1, 'RESTAURANT'), -- reservationId = 13
(2, 'RESTAURANT'), -- reservationId = 14
(3, 'RESTAURANT'), -- reservationId = 15
(3, 'RESTAURANT'), -- reservationId = 16
(5, 'RESTAURANT'), -- reservationId = 17
(6, 'RESTAURANT'), -- reservationId = 18
(7, 'RESTAURANT'); -- reservationId = 19


-- 교통 예약 테스트용 데이터
INSERT INTO transport_info (
    transport_id,
    departure_id, arrival_id, departure_name, arrival_name,
    departure_time, arrival_time, train_type, train_no,
    seat_type, seat_total, seat_remain, price, is_visible
) VALUES
      (1, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 10:00:00', '2025-08-01 12:30:00', 'ktx', 'KTX-801', 'general', 240, 240, 49800.00, 1),
      (2, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 10:00:00', '2025-08-01 12:30:00', 'ktx-sancheon', 'KTX-802', 'general', 240, 240, 49800.00, 1),
      (3, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 11:00:00', '2025-08-01 13:30:00', 'ktx', 'KTX-803', 'general', 240, 240, 49800.00, 1),
      (4, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 11:00:00', '2025-08-01 13:30:00', 'ktx-sancheon', 'KTX-804', 'general', 240, 240, 49800.00, 1),
      (5, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 12:00:00', '2025-08-01 14:30:00', 'ktx', 'KTX-805', 'general', 240, 240, 49800.00, 1),
      (6, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 12:00:00', '2025-08-01 14:30:00', 'ktx-sancheon', 'KTX-806', 'general', 240, 240, 49800.00, 1),
      (7, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 13:00:00', '2025-08-01 15:30:00', 'ktx', 'KTX-807', 'general', 240, 240, 49800.00, 1),
      (8, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 13:00:00', '2025-08-01 15:30:00', 'ktx-sancheon', 'KTX-808', 'general', 240, 240, 49800.00, 1),
      (9, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 14:00:00', '2025-08-01 16:30:00', 'ktx', 'KTX-809', 'general', 240, 240, 49800.00, 1),
      (10, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 14:00:00', '2025-08-01 16:30:00', 'ktx-sancheon', 'KTX-810', 'general', 240, 240, 49800.00, 1),
      (11, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 15:00:00', '2025-08-01 17:30:00', 'ktx', 'KTX-811', 'general', 240, 240, 49800.00, 1),
      (12, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 15:00:00', '2025-08-01 17:30:00', 'ktx-sancheon', 'KTX-812', 'general', 240, 240, 49800.00, 1),
      (13, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 16:00:00', '2025-08-01 18:30:00', 'ktx', 'KTX-813', 'general', 240, 240, 49800.00, 1),
      (14, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 16:00:00', '2025-08-01 18:30:00', 'ktx-sancheon', 'KTX-814', 'general', 240, 240, 49800.00, 1),
      (15, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-01 17:00:00', '2025-08-01 19:30:00', 'ktx', 'KTX-815', 'general', 240, 240, 49800.00, 1),
      (16, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-01 17:00:00', '2025-08-01 19:30:00', 'ktx-sancheon', 'KTX-816', 'general', 240, 240, 49800.00, 1),
      (17, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 10:00:00', '2025-08-02 12:30:00', 'ktx', 'KTX-817', 'general', 240, 240, 49800.00, 1),
      (18, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 10:00:00', '2025-08-02 12:30:00', 'ktx-sancheon', 'KTX-818', 'general', 240, 240, 49800.00, 1),
      (19, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 11:00:00', '2025-08-02 13:30:00', 'ktx', 'KTX-819', 'general', 240, 240, 49800.00, 1),
      (20, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 11:00:00', '2025-08-02 13:30:00', 'ktx-sancheon', 'KTX-820', 'general', 240, 240, 49800.00, 1),
      (21, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 12:00:00', '2025-08-02 14:30:00', 'ktx', 'KTX-821', 'general', 240, 240, 49800.00, 1),
      (22, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 12:00:00', '2025-08-02 14:30:00', 'ktx-sancheon', 'KTX-822', 'general', 240, 240, 49800.00, 1),
      (23, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 13:00:00', '2025-08-02 15:30:00', 'ktx', 'KTX-823', 'general', 240, 240, 49800.00, 1),
      (24, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 13:00:00', '2025-08-02 15:30:00', 'ktx-sancheon', 'KTX-824', 'general', 240, 240, 49800.00, 1),
      (25, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 14:00:00', '2025-08-02 16:30:00', 'ktx', 'KTX-825', 'general', 240, 240, 49800.00, 1),
      (26, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 14:00:00', '2025-08-02 16:30:00', 'ktx-sancheon', 'KTX-826', 'general', 240, 240, 49800.00, 1),
      (27, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 15:00:00', '2025-08-02 17:30:00', 'ktx', 'KTX-827', 'general', 240, 240, 49800.00, 1),
      (28, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 15:00:00', '2025-08-02 17:30:00', 'ktx-sancheon', 'KTX-828', 'general', 240, 240, 49800.00, 1),
      (29, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 16:00:00', '2025-08-02 18:30:00', 'ktx', 'KTX-829', 'general', 240, 240, 49800.00, 1),
      (30, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 16:00:00', '2025-08-02 18:30:00', 'ktx-sancheon', 'KTX-830', 'general', 240, 240, 49800.00, 1),
      (31, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-02 17:00:00', '2025-08-02 19:30:00', 'ktx', 'KTX-831', 'general', 240, 240, 49800.00, 1),
      (32, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-02 17:00:00', '2025-08-02 19:30:00', 'ktx-sancheon', 'KTX-832', 'general', 240, 240, 49800.00, 1),
      (33, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 10:00:00', '2025-08-03 12:30:00', 'ktx', 'KTX-833', 'general', 240, 240, 49800.00, 1),
      (34, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 10:00:00', '2025-08-03 12:30:00', 'ktx-sancheon', 'KTX-834', 'general', 240, 240, 49800.00, 1),
      (35, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 11:00:00', '2025-08-03 13:30:00', 'ktx', 'KTX-835', 'general', 240, 240, 49800.00, 1),
      (36, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 11:00:00', '2025-08-03 13:30:00', 'ktx-sancheon', 'KTX-836', 'general', 240, 240, 49800.00, 1),
      (37, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 12:00:00', '2025-08-03 14:30:00', 'ktx', 'KTX-837', 'general', 240, 240, 49800.00, 1),
      (38, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 12:00:00', '2025-08-03 14:30:00', 'ktx-sancheon', 'KTX-838', 'general', 240, 240, 49800.00, 1),
      (39, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 13:00:00', '2025-08-03 15:30:00', 'ktx', 'KTX-839', 'general', 240, 240, 49800.00, 1),
      (40, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 13:00:00', '2025-08-03 15:30:00', 'ktx-sancheon', 'KTX-840', 'general', 240, 240, 49800.00, 1),
      (41, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 14:00:00', '2025-08-03 16:30:00', 'ktx', 'KTX-841', 'general', 240, 240, 49800.00, 1),
      (42, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 14:00:00', '2025-08-03 16:30:00', 'ktx-sancheon', 'KTX-842', 'general', 240, 240, 49800.00, 1),
      (43, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 15:00:00', '2025-08-03 17:30:00', 'ktx', 'KTX-843', 'general', 240, 240, 49800.00, 1),
      (44, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 15:00:00', '2025-08-03 17:30:00', 'ktx-sancheon', 'KTX-844', 'general', 240, 240, 49800.00, 1),
      (45, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 16:00:00', '2025-08-03 18:30:00', 'ktx', 'KTX-845', 'general', 240, 240, 49800.00, 1),
      (46, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 16:00:00', '2025-08-03 18:30:00', 'ktx-sancheon', 'KTX-846', 'general', 240, 240, 49800.00, 1),
      (47, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-03 17:00:00', '2025-08-03 19:30:00', 'ktx', 'KTX-847', 'general', 240, 240, 49800.00, 1),
      (48, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-03 17:00:00', '2025-08-03 19:30:00', 'ktx-sancheon', 'KTX-848', 'general', 240, 240, 49800.00, 1),
      (49, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 10:00:00', '2025-08-04 12:30:00', 'ktx', 'KTX-849', 'general', 240, 240, 49800.00, 1),
      (50, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 10:00:00', '2025-08-04 12:30:00', 'ktx-sancheon', 'KTX-850', 'general', 240, 240, 49800.00, 1),
      (51, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 11:00:00', '2025-08-04 13:30:00', 'ktx', 'KTX-851', 'general', 240, 240, 49800.00, 1),
      (52, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 11:00:00', '2025-08-04 13:30:00', 'ktx-sancheon', 'KTX-852', 'general', 240, 240, 49800.00, 1),
      (53, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 12:00:00', '2025-08-04 14:30:00', 'ktx', 'KTX-853', 'general', 240, 240, 49800.00, 1),
      (54, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 12:00:00', '2025-08-04 14:30:00', 'ktx-sancheon', 'KTX-854', 'general', 240, 240, 49800.00, 1),
      (55, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 13:00:00', '2025-08-04 15:30:00', 'ktx', 'KTX-855', 'general', 240, 240, 49800.00, 1),
      (56, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 13:00:00', '2025-08-04 15:30:00', 'ktx-sancheon', 'KTX-856', 'general', 240, 240, 49800.00, 1),
      (57, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 14:00:00', '2025-08-04 16:30:00', 'ktx', 'KTX-857', 'general', 240, 240, 49800.00, 1),
      (58, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 14:00:00', '2025-08-04 16:30:00', 'ktx-sancheon', 'KTX-858', 'general', 240, 240, 49800.00, 1),
      (59, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 15:00:00', '2025-08-04 17:30:00', 'ktx', 'KTX-859', 'general', 240, 240, 49800.00, 1),
      (60, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 15:00:00', '2025-08-04 17:30:00', 'ktx-sancheon', 'KTX-860', 'general', 240, 240, 49800.00, 1),
      (61, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 16:00:00', '2025-08-04 18:30:00', 'ktx', 'KTX-861', 'general', 240, 240, 49800.00, 1),
      (62, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 16:00:00', '2025-08-04 18:30:00', 'ktx-sancheon', 'KTX-862', 'general', 240, 240, 49800.00, 1),
      (63, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-04 17:00:00', '2025-08-04 19:30:00', 'ktx', 'KTX-863', 'general', 240, 240, 49800.00, 1),
      (64, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-04 17:00:00', '2025-08-04 19:30:00', 'ktx-sancheon', 'KTX-864', 'general', 240, 240, 49800.00, 1),
      (65, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 10:00:00', '2025-08-05 12:30:00', 'ktx', 'KTX-865', 'general', 240, 240, 49800.00, 1),
      (66, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 10:00:00', '2025-08-05 12:30:00', 'ktx-sancheon', 'KTX-866', 'general', 240, 240, 49800.00, 1),
      (67, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 11:00:00', '2025-08-05 13:30:00', 'ktx', 'KTX-867', 'general', 240, 240, 49800.00, 1),
      (68, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 11:00:00', '2025-08-05 13:30:00', 'ktx-sancheon', 'KTX-868', 'general', 240, 240, 49800.00, 1),
      (69, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 12:00:00', '2025-08-05 14:30:00', 'ktx', 'KTX-869', 'general', 240, 240, 49800.00, 1),
      (70, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 12:00:00', '2025-08-05 14:30:00', 'ktx-sancheon', 'KTX-870', 'general', 240, 240, 49800.00, 1),
      (71, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 13:00:00', '2025-08-05 15:30:00', 'ktx', 'KTX-871', 'general', 240, 240, 49800.00, 1),
      (72, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 13:00:00', '2025-08-05 15:30:00', 'ktx-sancheon', 'KTX-872', 'general', 240, 240, 49800.00, 1),
      (73, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 14:00:00', '2025-08-05 16:30:00', 'ktx', 'KTX-873', 'general', 240, 240, 49800.00, 1),
      (74, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 14:00:00', '2025-08-05 16:30:00', 'ktx-sancheon', 'KTX-874', 'general', 240, 240, 49800.00, 1),
      (75, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 15:00:00', '2025-08-05 17:30:00', 'ktx', 'KTX-875', 'general', 240, 240, 49800.00, 1),
      (76, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 15:00:00', '2025-08-05 17:30:00', 'ktx-sancheon', 'KTX-876', 'general', 240, 240, 49800.00, 1),
      (77, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 16:00:00', '2025-08-05 18:30:00', 'ktx', 'KTX-877', 'general', 240, 240, 49800.00, 1),
      (78, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 16:00:00', '2025-08-05 18:30:00', 'ktx-sancheon', 'KTX-878', 'general', 240, 240, 49800.00, 1),
      (79, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-05 17:00:00', '2025-08-05 19:30:00', 'ktx', 'KTX-879', 'general', 240, 240, 49800.00, 1),
      (80, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-05 17:00:00', '2025-08-05 19:30:00', 'ktx-sancheon', 'KTX-880', 'general', 240, 240, 49800.00, 1),
      (81, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 10:00:00', '2025-08-06 12:30:00', 'ktx', 'KTX-881', 'general', 240, 240, 49800.00, 1),
      (82, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 10:00:00', '2025-08-06 12:30:00', 'ktx-sancheon', 'KTX-882', 'general', 240, 240, 49800.00, 1),
      (83, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 11:00:00', '2025-08-06 13:30:00', 'ktx', 'KTX-883', 'general', 240, 240, 49800.00, 1),
      (84, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 11:00:00', '2025-08-06 13:30:00', 'ktx-sancheon', 'KTX-884', 'general', 240, 240, 49800.00, 1),
      (85, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 12:00:00', '2025-08-06 14:30:00', 'ktx', 'KTX-885', 'general', 240, 240, 49800.00, 1),
      (86, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 12:00:00', '2025-08-06 14:30:00', 'ktx-sancheon', 'KTX-886', 'general', 240, 240, 49800.00, 1),
      (87, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 13:00:00', '2025-08-06 15:30:00', 'ktx', 'KTX-887', 'general', 240, 240, 49800.00, 1),
      (88, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 13:00:00', '2025-08-06 15:30:00', 'ktx-sancheon', 'KTX-888', 'general', 240, 240, 49800.00, 1),
      (89, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 14:00:00', '2025-08-06 16:30:00', 'ktx', 'KTX-889', 'general', 240, 240, 49800.00, 1),
      (90, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 14:00:00', '2025-08-06 16:30:00', 'ktx-sancheon', 'KTX-890', 'general', 240, 240, 49800.00, 1),
      (91, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 15:00:00', '2025-08-06 17:30:00', 'ktx', 'KTX-891', 'general', 240, 240, 49800.00, 1),
      (92, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 15:00:00', '2025-08-06 17:30:00', 'ktx-sancheon', 'KTX-892', 'general', 240, 240, 49800.00, 1),
      (93, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 16:00:00', '2025-08-06 18:30:00', 'ktx', 'KTX-893', 'general', 240, 240, 49800.00, 1),
      (94, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 16:00:00', '2025-08-06 18:30:00', 'ktx-sancheon', 'KTX-894', 'general', 240, 240, 49800.00, 1),
      (95, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-06 17:00:00', '2025-08-06 19:30:00', 'ktx', 'KTX-895', 'general', 240, 240, 49800.00, 1),
      (96, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-06 17:00:00', '2025-08-06 19:30:00', 'ktx-sancheon', 'KTX-896', 'general', 240, 240, 49800.00, 1),
      (97, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 10:00:00', '2025-08-07 12:30:00', 'ktx', 'KTX-897', 'general', 240, 240, 49800.00, 1),
      (98, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 10:00:00', '2025-08-07 12:30:00', 'ktx-sancheon', 'KTX-898', 'general', 240, 240, 49800.00, 1),
      (99, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 11:00:00', '2025-08-07 13:30:00', 'ktx', 'KTX-899', 'general', 240, 240, 49800.00, 1),
      (100, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 11:00:00', '2025-08-07 13:30:00', 'ktx-sancheon', 'KTX-900', 'general', 240, 240, 49800.00, 1),
      (101, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 12:00:00', '2025-08-07 14:30:00', 'ktx', 'KTX-901', 'general', 240, 240, 49800.00, 1),
      (102, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 12:00:00', '2025-08-07 14:30:00', 'ktx-sancheon', 'KTX-902', 'general', 240, 240, 49800.00, 1),
      (103, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 13:00:00', '2025-08-07 15:30:00', 'ktx', 'KTX-903', 'general', 240, 240, 49800.00, 1),
      (104, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 13:00:00', '2025-08-07 15:30:00', 'ktx-sancheon', 'KTX-904', 'general', 240, 240, 49800.00, 1),
      (105, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 14:00:00', '2025-08-07 16:30:00', 'ktx', 'KTX-905', 'general', 240, 240, 49800.00, 1),
      (106, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 14:00:00', '2025-08-07 16:30:00', 'ktx-sancheon', 'KTX-906', 'general', 240, 240, 49800.00, 1),
      (107, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 15:00:00', '2025-08-07 17:30:00', 'ktx', 'KTX-907', 'general', 240, 240, 49800.00, 1),
      (108, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 15:00:00', '2025-08-07 17:30:00', 'ktx-sancheon', 'KTX-908', 'general', 240, 240, 49800.00, 1),
      (109, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 16:00:00', '2025-08-07 18:30:00', 'ktx', 'KTX-909', 'general', 240, 240, 49800.00, 1),
      (110, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 16:00:00', '2025-08-07 18:30:00', 'ktx-sancheon', 'KTX-910', 'general', 240, 240, 49800.00, 1),
      (111, 'SEOUL_STN', 'BUSAN_STN', '서울역', '부산역', '2025-08-07 17:00:00', '2025-08-07 19:30:00', 'ktx', 'KTX-911', 'general', 240, 240, 49800.00, 1),
      (112, 'BUSAN_STN', 'SEOUL_STN', '부산역', '서울역', '2025-08-07 17:00:00', '2025-08-07 19:30:00', 'ktx-sancheon', 'KTX-912', 'general', 240, 240, 49800.00, 1);


-- 식당 정보 테스트용 데이터
INSERT INTO restaurant_info (
    rest_name, address, category, rest_image_url,
    phone, description, latitude, longitude
) VALUES
-- 한식 (rest_id: 1 ~ 3)
('해운대 곰장어집', '부산광역시 해운대구 중동2로 10', 'korean',
 'https://png.pngtree.com/thumb_back/fh260/background/20210910/pngtree-dining-room-at-night-image_842471.jpg',
 '051-111-2222', '불맛 가득한 곰장어 전문점', 35.163, 129.163),

('부산 밀면집', '부산광역시 동래구 충렬대로 237', 'korean',
 'https://media.timeout.com/images/102190657/750/562/image.jpg',
 '051-333-4444', '시원한 육수의 전통 밀면집', 35.205, 129.086),

('돼지국밥천국', '부산광역시 부산진구 중앙대로 708', 'korean',
 'https://s3.qplace.kr/portfolio/2933/4ca799bb3e3813a87258fb32f1dd0a9e_w800.webp',
 '051-555-6666', '진한 육수의 돼지국밥 명가', 35.152, 129.060),

-- 일식 (4 ~ 6)
('스시하루 부산점', '부산광역시 수영구 수영로 570', 'japanese',
 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdAn8yesg8uOqwEKgJZIjzKhSC8HMbtY_ykA&s',
 '051-777-8888', '정통 오마카세 전문점', 35.160, 129.115),

('이자카야 코이', '부산광역시 남구 용소로 45', 'japanese',
 'https://media.timeout.com/images/102190657/750/562/image.jpg',
 '051-222-3333', '감성 가득한 이자카야', 35.128, 129.093),

('돈카츠연구소', '부산광역시 해운대구 해운대로 569', 'japanese',
 'https://www.qplace.kr/content/images/2023/11/9_4247.jpg',
 '051-444-5555', '두툼한 수제 돈카츠', 35.162, 129.163),

-- 중식 (7 ~ 9)
('팔선생 중화요리', '부산광역시 부산진구 동천로 92', 'chinese',
 'https://s3.qplace.kr/portfolio/4186/4f53dc96a5ab531a78c68316ea57daf4_w800.webp',
 '051-123-1234', '짜장면·짬뽕 명가', 35.160, 129.065),

('홍콩반점41 부산대점', '부산광역시 금정구 장전온천천로 66', 'chinese',
 'https://s3.qplace.kr/portfolio/4241/ab1d0a81a28848fc72f5f9a7e45129f8_w800.webp',
 '051-321-4321', '프랜차이즈 중식당', 35.230, 129.089),

('루이하오 중국요리', '부산광역시 연제구 중앙대로 1156', 'chinese',
 'https://s3.qplace.kr/portfolio/4241/b0a6ff000a70a3fc24529f68491cf02c_w800.webp',
 '051-444-2222', '정통 중식당', 35.181, 129.081),

-- 양식 (10 ~ 12)
('부산 파스타하우스', '부산광역시 수영구 광안해변로 193', 'western',
 'https://s3.qplace.kr/portfolio/4382/5f12bdc3fdb449d7d51dfe3b460099ef_w800.webp',
 '051-123-4567', '오션뷰 감성 파스타 맛집', 35.153, 129.118),

('스테이크 팩토리', '부산광역시 남구 분포로 145', 'western',
 'https://s3.qplace.kr/portfolio/2933/4ca799bb3e3813a87258fb32f1dd0a9e_w800.webp',
 '051-999-8888', '두툼한 스테이크 전문점', 35.127, 129.100),

('마리나 피자', '부산광역시 해운대구 우동 1418-2', 'western',
 'https://s3.qplace.kr/portfolio/2933/d79f6181e3c2e6adb05d34a2c60afe49_w800.webp',
 '051-555-7777', '화덕 피자 전문점', 35.164, 129.163),

-- 기타 (13 ~ 15)
('비건그린 키친', '부산광역시 동구 중앙대로 248', 'etc',
 'https://street-h.com/wp-content/uploads/2021/03/pyeongsangshi.jpg',
 '051-000-1111', '채식주의자를 위한 건강식당', 35.137, 129.059),

('버블티&카페', '부산광역시 해운대구 좌동로 63', 'etc',
 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRBGoWdfQH9Px179V7bEj1LXefV9XTrS_EwjA&s',
 '051-321-8765', '음료와 디저트 전문 카페', 35.167, 129.176),

('이색분식연구소', '부산광역시 중구 광복로 12', 'etc',
 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRoGoVsFpGXiWMTgYnIhE-j37LpzjtOtde_iA&s',
 '051-888-1111', '퓨전 분식 전문점', 35.101, 129.033);


-- 식당 시간 테스트용 데이터
-- 식당별 예약 시간 슬롯 생성 (rest_id: 1 ~ 15, 시간: 11시 ~ 19시, max_capacity: 5)
INSERT INTO rest_time_slot (rest_id, res_time, max_capacity) VALUES
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


-- 식당 예약 테스트용 데이터
INSERT INTO rest_res (rest_id, reservation_id, trip_day_id, res_num, rest_time_id) VALUES
(1, 13, 1, 1, 8), -- 1일 18시 1명 해운대 곰장어집
(7, 14, 2, 1, 57), -- 2일 13시 1명 팔선생 중화요리
(15, 15, 3, 1, 128), -- 3일 12시 1명 이색분식연구소
(5, 16, 3, 1, 45), -- 3일 19시 1명 이자카야 코지
(5, 17, 5, 3, 44), -- 3일 18시 3명 이자카야 코지
(10, 18, 6, 3, 84), -- 4일 13시 3명 부산 파스타하우스
(15, 19, 7, 2, 127); -- 5일 11시 2명 이색분식연구소


-- 비용 테스트용 데이터
INSERT INTO expense(trip_id, member_id, expense_name, expense_date, amount, location, settlement_completed)
VALUES (2, 2, '교통비', '2025-08-03 17:10:00' ,99600, 'BUSAN', false),
       (2, 2, '숙박 비용 롯데호텔', '2025-08-03 18:10:00' ,193000, 'BUSAN', false),
       (2, 2, '1일차 저녁 파스타', '2025-08-03 20:00:00', 54000, 'BUSAN', false),
       (2, 3, '2일차 아침 돼지 국밥', '2025-08-04 21:10:00' ,45000, 'BUSAN', false),
       (2, 4, '2일차 점심 부산 밀면', '2025-08-04 21:10:00' ,30000, 'BUSAN', false),
       (2, 2, '2일차 저녁 부산 꼼장어', '2025-08-04 21:10:00' ,50000, 'BUSAN', false),
       (2, 2, '숙박 비용 시그니엘 부산', '2025-08-04 21:10:00' ,300000, 'BUSAN', false),
       (2, 3, '3일차 아침 이재모 피자', '2025-08-05 11:00:00' ,40000, 'BUSAN', false),
       (2, 4, '버블티&카페', '2025-08-05 20:10:00' ,18000, 'BUSAN', false),
       (2, 4, '교통비', '2025-08-05 21:10:00' ,99600, 'BUSAN', false);


-- 정산 내역 데이터
INSERT INTO settlement_notes (expense_id, trip_id, member_id, share_amount, is_payed, received, created_at)
VALUES
-- (2, 2, '교통비', 149400, 'BUSAN', false)에 대한 정산 (윈터가 계산, 윈터+지젤 교통비)
(1, 2, 2, 49800, true, false, '2025-08-03 17:10:00'),
(1, 2, 3, 49800, false, true, '2025-08-03 17:10:00'),
-- (2, 2, '숙박 비용 롯데호텔', 193000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(2, 2, 2, 64334, true, false, '2025-08-03 18:10:00'),
(2, 2, 3, 64333, false, true, '2025-08-03 18:10:00'),
(2, 2, 4, 64333, false, true, '2025-08-03 18:10:00'),
-- (2, 2, '1일차 저녁 파스타', 54000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(2, 2, 2, 18000, true, false, '2025-08-03 20:00:00'),
(2, 2, 3, 18000, false, true, '2025-08-03 20:00:00'),
(2, 2, 4, 18000, false, true, '2025-08-03 20:00:00'),
-- (2, 3, '돼지 국밥', 45000, 'BUSAN', false)에 대한 정산 (지젤이 계산)
(3, 2, 2, 15000, false, true, '2025-08-04 21:10:00'),
(3, 2, 3, 15000, true, false, '2025-08-04 21:10:00'),
(3, 2, 4, 15000, false, true, '2025-08-04 21:10:00'),
-- (2, 2, '부산 밀면', 30000, 'BUSAN', false)에 대한 정산 (닝닝이 계산)
(4, 2, 2, 10000, false, true, '2025-08-04 21:10:00'),
(4, 2, 3, 10000, false, true, '2025-08-04 21:10:00'),
(4, 2, 4, 10000, true, false, '2025-08-04 21:10:00'),
-- (2, 2, '부산 꼼장어', 50000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(5, 2, 2, 16667, true, false, '2025-08-04 21:10:00'),
(5, 2, 3, 16666, false, true, '2025-08-04 21:10:00'),
(5, 2, 4, 16666, false, true, '2025-08-04 21:10:00'),
-- (2, 2, '숙박 비용 시그니엘 부산', 300000, 'BUSAN', false)에 대한 정산 (윈터가 계산)
(6, 2, 2, 100000, true, false, '2025-08-04 21:10:00'),
(6, 2, 3, 100000, false, true, '2025-08-04 21:10:00'),
(6, 2, 4, 100000, false, true, '2025-08-04 21:10:00'),
-- (2, 3, '이재모 피자', 40000, 'BUSAN', false)에 대한 정산 (닝닝이 계산)
(7, 2, 2, 13334, false, true, '2025-08-05 11:00:00'),
(7, 2, 3, 13333, true, false, '2025-08-05 11:00:00'),
(7, 2, 4, 13333, false, true, '2025-08-05 11:00:00'),
-- (2, 2, '버블티&카페', 18000, 'BUSAN', false)에 대한 정산 (지젤이 계산, 윈터는 먼저 감 ㅜ)
(8, 2, 3, 9000, false, true, '2025-08-05 20:10:00'),
(8, 2, 4, 9000, true, false, '2025-08-05 20:10:00'),
-- (2, 2, '교통비', 149400, 'BUSAN', false)에 대한 정산 (닝닝이 계산)
(9, 2, 3, 49800, false, true, '2025-08-05 21:10:00'),
(9, 2, 4, 49800, true, false, '2025-08-05 21:10:00');


-- 여행 기록 데이터
INSERT INTO trip_records (trip_id, member_id, title, record_date, content)
VALUES (1, 1, '부산 도착~', '2025-08-01', '내일 이재모 피자 먹어야지 ㅎㅎ 숙소도 너무 좋다'),
       (1, 1, '이재모 피자', '2025-08-02', '진짜 맛있음. 다음엔 다른 메뉴 먹어봐야지'),
       (1, 1, '남포동 투어', '2025-08-02', null),
       (1, 1, '롯데호텔 조식', '2025-08-03', '짱 맛있음'),
       (1, 1, '해운대', '2025-08-03', '더운데 경치가 너무 좋았다~~'),
       (1, 1, '부산 마지막날 ㅜㅜ', '2025-08-04', '아쉽다. 다음에 또 와야지');

-- 여행 기록 데이터
INSERT INTO trip_record_images (record_id, image_url)
VALUES (1, '2b1d7834-3f92-4fb8-8e39-dac2c952c6f4.webp'),
       (1, '1482b39b-981d-43d9-9747-446f0c08c545.jpg'),
       (2, '73eed0e4-cfeb-4fad-b890-45a95affb559.webp'),
       (2, '6078349e-db19-4a53-9d82-f9b2e81cbee3.png'),
       (3, '773215b6-3a11-4dc5-8050-234d3aaf7b78.webp'),
       (4, 'b290a04d-d05c-4975-9d52-cd56138bdaf5.jpg'),
       (5, '7fb93cf9-02d4-4678-8a6b-d30371dfcf75.jpg');

SET FOREIGN_KEY_CHECKS = 1;