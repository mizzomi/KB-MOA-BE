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

-- Security Audit Log 테이블
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
    trip_location ENUM ('부산', '강릉', '제주', '서울', '대구', '대전', '광주', '목포') NOT NULL,
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
    location_name ENUM ('부산', '강릉', '제주', '서울', '대구', '대전', '광주', '목포' ) NOT NULL,
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
    rest_image_url VARCHAR(255)                                                                  NOT NULL,
    phone          VARCHAR(20)                                                                   NULL,
    description    TEXT                                                                          NULL,
    latitude       DECIMAL(10, 7)                                                                NULL,
    longitude      DECIMAL(10, 7)                                                                NULL,
    menu_url       VARCHAR(255)                                                                  NULL,
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
INSERT INTO member (
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
INSERT INTO owner (
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
VALUES (1, 1, '편의점 간식', 4500.00, '2025-08-01 09:10:00', '부산'),
       (1, 1, '부산 파스타하우스', 21000.00, '2025-08-03 12:43:25', '부산'),
       (1, 1, '카페 커피', 6500.00, '2025-08-05 15:40:00', '부산');

-- 윈터 (member_id=2)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (2, 2, '주유소 결제', 54000.00, '2025-08-03 10:20:00', '부산'),
       (2, 2, '숙박 비용', 193000.00, '2025-08-03 12:00:00', '부산'),
       (2, 2, '교통비', 105000.00, '2025-08-04 13:30:00', '부산'),
       (2, 2, '길거리 간식', 9999.00, '2025-08-04 17:30:00', '부산'),
       (2, 2, '편의점 물품', 12000.00, '2025-08-05 22:10:00', '부산');

-- 지젤 (member_id=3)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (3, 3, '팔선생 중화요리', 61000.00, '2025-08-04 12:56:43', '부산'),
       (3, 3, '이색분식연구소', 10000.00, '2025-08-04 19:44:26', '부산'),
       (3, 3, '기념품 구매', 22000.00, '2025-08-04 11:15:00', '부산'),
       (3, 3, '카페 디저트', 8500.00, '2025-08-04 15:45:00', '부산');

-- 닝닝 (member_id=4)
INSERT INTO payment_record (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (4, 4, '이자카야 코이', 24300.00, '2025-08-03 18:35:47', '부산'),
       (4, 4, '편의점 간식', 5500.00, '2025-08-04 12:20:00', '부산'),
       (4, 4, '기념품', 17000.00, '2025-08-05 16:30:00', '부산');


-- 여행 테스트용 데이터(카리나 1인여행 8/1 ~ 8/4)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '혼자 부산 여행', '부산', '2025-08-01', '2025-08-04');

-- 여행 테스트용 데이터(윈터, 지젤, 닝닝 3인여행 8/3 ~ 8/5)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '셋이서 부산 여행', '부산', '2025-08-03', '2025-08-05');

-- 여행 테스트용 데이터 기존 2개 + 여행 15개 추가 -> 총 17개
-- (카리나, 윈터, 지젤, 닝닝 | MOKPO 여행 1)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '에스파, 낭만을 걷다 - 목포 편', '목포', '2025-09-29', '2025-09-30');

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 2)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, 'aespa in Busan: 파도와 함께', '부산', '2025-03-16', '2025-03-19');

-- (카리나, 윈터, 지젤, 닝닝 | DAEJEON 여행 3)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (3, '한적한 오후, 대전 산책', '대전', '2025-10-04', '2025-10-07');

-- (카리나, 윈터, 지젤, 닝닝 | DAEGU 여행 4)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '햇살 가득한 대구, 여름의 기억', '대구', '2025-06-11', '2025-06-14');

-- (카리나, 윈터, 지젤, 닝닝 | GWANGJU 여행 5)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '광주에서 찾은 작은 평화', '광주', '2025-05-10', '2025-05-12');

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 6)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '푸른 바다, 다시 부산에서', '부산', '2025-10-10', '2025-10-13');

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 7)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '서울 감성충전 투어', '서울', '2025-08-13', '2025-08-14');

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 8)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '서울 속 서울 여행', '서울', '2025-09-01', '2025-09-02');

-- (닝닝, 윈터 | DAEJEON 여행 9)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (4, '성심당을 위한 대전 여행', '대전', '2025-07-21', '2025-07-24');

-- (윈터, 카리나 | JEJU 여행 10)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (2, '윈터랑 카리나의 제주 여행', '제주', '2025-10-25', '2025-10-27');

-- (카리나, 지젤, 윈터 | JEJU 여행 11)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '닝닝 없는 제주 여행', '제주', '2025-05-16', '2025-05-18');

-- (카리나, 지젤 | BUSAN 여행 12)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '부산 맛집 탐방기', '부산', '2025-08-08', '2025-08-10');

-- (닝닝, 지젤 | GANGNEUNG 여행 13)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (4, '바다 보고 싶어서, 강릉', '강릉', '2025-04-13', '2025-04-15');

-- (카리나, 닝닝 | DAEJEON 여행 14)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, '가볍게 떠난 대전 소풍', '대전', '2025-11-10', '2025-11-11');

-- (지젤, 닝닝, 윈터 | BUSAN 여행 15)
INSERT INTO trip (member_id, trip_name, trip_location, start_date, end_date)
VALUES (3, '세 명이서 부산 미식여행', '부산', '2025-09-16', '2025-09-18');





INSERT INTO trip_location (location_name, latitude, longitude, address)
VALUES ('부산', 35.179554, 129.075642, '부산광역시 중구 중앙대로 100'),
       ('강릉', 37.751853, 128.876057, '강원특별자치도 강릉시 교동광장로 100'),
       ('제주', 33.499621, 126.531188, '제주특별자치도 제주시 중앙로 100'),
       ('서울', 37.566535, 126.977969, '서울특별시 중구 세종대로 110'),
       ('대구', 35.87222, 128.6025, '대구광역시 중구 공평로 88'),
       ('대전', 36.35041, 127.38455, '대전광역시 중구 중앙로 101'),
       ('광주', 35.15954, 126.8526, '광주광역시 서구 상무대로 100'),
       ('목포', 34.81184, 126.39257, '전라남도 목포시 영산로 100');



INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES  (1, 1, 'HOST', NOW()), -- 카리나 호스트 부산여행
        (2, 2, 'HOST', NOW()), -- 윈터 호스트 부산여행
        (2, 3, 'MEMBER', NOW()), -- 닝닝 멤버 부산여행
        (2, 4, 'MEMBER', NOW()); -- 지젤 멤버 부산여행
-- (카리나, 윈터, 지젤, 닝닝 | MOKPO 여행 1)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (3, 1, 'HOST', NOW()),
       (3, 2, 'MEMBER', NOW()),
       (3, 3, 'MEMBER', NOW()),
       (3, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 2)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (4, 1, 'HOST', NOW()),
       (4, 2, 'MEMBER', NOW()),
       (4, 3, 'MEMBER', NOW()),
       (4, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | DAEJEON 여행 3)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (5, 3, 'HOST', NOW()),
       (5, 1, 'MEMBER', NOW()),
       (5, 2, 'MEMBER', NOW()),
       (5, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | DAEGU 여행 4)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (6, 2, 'HOST', NOW()),
       (6, 1, 'MEMBER', NOW()),
       (6, 3, 'MEMBER', NOW()),
       (6, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | GWANGJU 여행 5)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (7, 1, 'HOST', NOW()),
       (7, 2, 'MEMBER', NOW()),
       (7, 3, 'MEMBER', NOW()),
       (7, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 6)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (8, 2, 'HOST', NOW()),
       (8, 1, 'MEMBER', NOW()),
       (8, 3, 'MEMBER', NOW()),
       (8, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 7)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (9, 1, 'HOST', NOW()),
       (9, 2, 'MEMBER', NOW()),
       (9, 3, 'MEMBER', NOW()),
       (9, 4, 'MEMBER', NOW());

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 8)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (10, 2, 'HOST', NOW()),
       (10, 1, 'MEMBER', NOW()),
       (10, 3, 'MEMBER', NOW()),
       (10, 4, 'MEMBER', NOW());

-- (닝닝, 윈터 | DAEJEON 여행 9)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (11, 4, 'HOST', NOW()),
       (11, 2, 'MEMBER', NOW());

-- (윈터, 카리나 | JEJU 여행 10)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (12, 2, 'HOST', NOW()),
       (12, 1, 'MEMBER', NOW());

-- (카리나, 지젤, 윈터 | JEJU 여행 11)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (13, 1, 'HOST', NOW()),
       (13, 3, 'MEMBER', NOW()),
       (13, 2, 'MEMBER', NOW());

-- (카리나, 지젤 | BUSAN 여행 12)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (14, 1, 'HOST', NOW()),
       (14, 3, 'MEMBER', NOW());

-- (닝닝, 지젤 | GANGNEUNG 여행 13)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (15, 4, 'HOST', NOW()),
       (15, 3, 'MEMBER', NOW());

-- (카리나, 닝닝 | DAEJEON 여행 14)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (16, 1, 'HOST', NOW()),
       (16, 4, 'MEMBER', NOW());

-- (지젤, 닝닝, 윈터 | BUSAN 여행 15)
INSERT INTO trip_member (trip_id, member_id, role, joined_at)
VALUES (17, 3, 'HOST', NOW()),
       (17, 4, 'MEMBER', NOW()),
       (17, 2, 'MEMBER', NOW());



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
-- 여행 날짜 INSERT 추가
-- (카리나, 윈터, 지젤, 닝닝 | MOKPO 여행 1)
INSERT INTO trip_day (trip_id, day)
VALUES (3, '2025-09-29'),
       (3, '2025-09-30');

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 2)
INSERT INTO trip_day (trip_id, day)
VALUES (4, '2025-03-16'),
       (4, '2025-03-17'),
       (4, '2025-03-18'),
       (4, '2025-03-19');

-- (카리나, 윈터, 지젤, 닝닝 | DAEJEON 여행 3)
INSERT INTO trip_day (trip_id, day)
VALUES (5, '2025-10-04'),
       (5, '2025-10-05'),
       (5, '2025-10-06'),
       (5, '2025-10-07');

-- (카리나, 윈터, 지젤, 닝닝 | DAEGU 여행 4)
INSERT INTO trip_day (trip_id, day)
VALUES (6, '2025-06-11'),
       (6, '2025-06-12'),
       (6, '2025-06-13'),
       (6, '2025-06-14');

-- (카리나, 윈터, 지젤, 닝닝 | GWANGJU 여행 5)
INSERT INTO trip_day (trip_id, day)
VALUES (7, '2025-05-10'),
       (7, '2025-05-11'),
       (7, '2025-05-12');

-- (카리나, 윈터, 지젤, 닝닝 | BUSAN 여행 6)
INSERT INTO trip_day (trip_id, day)
VALUES (8, '2025-10-10'),
       (8, '2025-10-11'),
       (8, '2025-10-12'),
       (8, '2025-10-13');

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 7)
INSERT INTO trip_day (trip_id, day)
VALUES (9, '2025-08-13'),
       (9, '2025-08-14');

-- (카리나, 윈터, 지젤, 닝닝 | SEOUL 여행 8)
INSERT INTO trip_day (trip_id, day)
VALUES (10, '2025-09-01'),
       (10, '2025-09-02');

-- (닝닝, 윈터 | DAEJEON 여행 9)
INSERT INTO trip_day (trip_id, day)
VALUES (11, '2025-07-21'),
       (11, '2025-07-22'),
       (11, '2025-07-23'),
       (11, '2025-07-24');

-- (윈터, 카리나 | JEJU 여행 10)
INSERT INTO trip_day (trip_id, day)
VALUES (12, '2025-10-25'),
       (12, '2025-10-26'),
       (12, '2025-10-27');

-- (카리나, 지젤, 윈터 | JEJU 여행 11)
INSERT INTO trip_day (trip_id, day)
VALUES (13, '2025-05-16'),
       (13, '2025-05-17'),
       (13, '2025-05-18');

-- (카리나, 지젤 | BUSAN 여행 12)
INSERT INTO trip_day (trip_id, day)
VALUES (14, '2025-08-08'),
       (14, '2025-08-09'),
       (14, '2025-08-10');

-- (닝닝, 지젤 | GANGNEUNG 여행 13)
INSERT INTO trip_day (trip_id, day)
VALUES (15, '2025-04-13'),
       (15, '2025-04-14'),
       (15, '2025-04-15');

-- (카리나, 닝닝 | DAEJEON 여행 14)
INSERT INTO trip_day (trip_id, day)
VALUES (16, '2025-11-10'),
       (16, '2025-11-11');

-- (지젤, 닝닝, 윈터 | BUSAN 여행 15)
INSERT INTO trip_day (trip_id, day)
VALUES (17, '2025-09-16'),
       (17, '2025-09-17'),
       (17, '2025-09-18');



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
(1, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/1 서울역 -> 부산역 10:00 출발 reservationId = 8 , tripDayId = 1
(4, 'TRANSPORT'), -- (카리나 1인 부산여행) 8/4 부산역 -> 서울역 16:00 출발 reservationId = 9, tripDayId = 4
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/3 서울역 -> 부산역 11:00 출발 (윈터, 지젤) reservationId = 10, tripDayId = 5
(5, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/5 서울역 -> 부산역 12:00 출발 (닝닝) reservationId = 11, tripDayId = 5
(7, 'TRANSPORT'), -- (윈터, 지젤, 닝닝 부산여행) 8/7 부산역 -> 서울역 10:00 출발 (윈터 스케줄 바쁨) reservationId = 12, tripDayId = 7
(7, 'TRANSPORT'); -- (윈터, 지젤, 닝닝 부산여행) 8/7 부산역 -> 서울역 15:00 출발 (지젤, 닝닝 느긋) reservationId = 13, tripDayId = 7


-- 교통 예약 테스트 데이터
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


INSERT INTO tran_res (transport_id, reservation_id, trip_day_id, departure, arrival, seat_room_no, seat_number, seat_type, booked_at, price, version, status)
VALUES
-- 카리나 혼자여행
(1, 8, 1, 'SEOUL_STN', 'BUSAN_STN', 10, '12A', 'general', '2025-08-01 09:50:00', 49800, 0, 'CONFIRMED'),
(62, 9, 4, 'BUSAN_STN', 'SEOUL_STN', 8, '3C', 'general', '2025-08-04 15:50:00', 49800, 0, 'CONFIRMED'),
-- 윈닝젤 여행
(35, 10, 5, 'SEOUL_STN', 'BUSAN_STN', 5, '12A', 'general', '2025-08-03 10:50:00', 49800, 0, 'CONFIRMED'),
(35, 10, 5, 'SEOUL_STN', 'BUSAN_STN', 5, '12B', 'general', '2025-08-03 10:50:00', 49800, 0, 'CONFIRMED'),
(37, 11, 5, 'SEOUL_STN', 'BUSAN_STN', 5, '12C', 'general', '2025-08-03 11:50:00', 49800, 0, 'CONFIRMED'),
(100, 12, 7, 'BUSAN_STN', 'SEOUL_STN', 5, '12A', 'general', '2025-08-05 09:50:00', 49800, 0, 'CONFIRMED'),
(102, 13, 7, 'BUSAN_STN', 'SEOUL_STN', 5, '12B', 'general', '2025-08-05 14:50:00', 49800, 0, 'CONFIRMED'),
(102, 13, 7, 'BUSAN_STN', 'SEOUL_STN', 5, '12C', 'general', '2025-08-05 14:50:00', 49800, 0, 'CONFIRMED');

SET FOREIGN_KEY_CHECKS = 1;