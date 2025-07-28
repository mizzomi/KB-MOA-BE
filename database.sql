-- 초기화
SET FOREIGN_KEY_CHECKS = 0;

-- DROP TABLES (FK 자식부터 → 부모순)
DROP TABLE IF EXISTS TRIP_RECORDS_IMAGES;
DROP TABLE IF EXISTS TRIP_RECORDS;
DROP TABLE IF EXISTS SETTLEMENT_NOTES;
DROP TABLE IF EXISTS EXPENSE;
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
    id_card_number VARCHAR(255)                                   NOT NULL UNIQUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- 회원 테스트용 데이터
INSERT INTO MEMBER (member_id, member_type, email, password, name, id_card_number)
VALUES (1, 'ROLE_USER', 'karina@test.com', '1234', '카리나', '0004114000001'),
       (2, 'ROLE_USER', 'winter@test.com', '1234', '윈터', '0101014000002'),
       (3, 'ROLE_USER', 'giselle@test.com', '1234', '지젤', '0010304000003'),
       (4, 'ROLE_USER', 'ningning@test.com', '1234', '닝닝', '0210234000002');



-- ========================================================================================
-- 사업자 테이블
-- ========================================================================================
CREATE TABLE OWNER
(
    owner_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_password VARCHAR(255) NOT NULL,
    owner_no       VARCHAR(255) NOT NULL
);
-- 사업자 테스트용 데이터
INSERT INTO OWNER (owner_id, owner_password, owner_no)
VALUES (1, 'owner1234', '123-45-67890'),
       (2, 'owner1234', '987-65-43210'),
       (3, 'owner1234', '456-78-90123');



-- ========================================================================================
-- 알림 테이블
-- ========================================================================================
CREATE TABLE NOTIFICATION
(
    notification_id   BIGINT                                                                              NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 알림 ID (PK)
    member_id         BIGINT                                                                              NOT NULL,                            -- 수신자 ID (FK)
    notification_type ENUM ('travel_invite', 'settlement_request', 'room_reserved', 'transport_reserved') NULL,                                -- 알림 유형
    title             VARCHAR(100)                                                                        NULL,                                -- 알림 제목
    content           TEXT                                                                                NULL,                                -- 알림 내용
    is_read           BOOLEAN                                                                             NOT NULL DEFAULT FALSE,              -- 읽음 여부
    created_at        TIMESTAMP                                                                           NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 생성일시 (자동 등록)

    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)                                                                                      -- 수신자 ID 외래키
);
-- 알림 테스트용 데이터
INSERT INTO NOTIFICATION (member_id, notification_type, title, content, is_read)
VALUES (3, 'travel_invite', '여행 초대', '윈터님이 강릉 여행에 초대했습니다.', FALSE),
       (4, 'settlement_request', '정산 요청', '윈터님이 숙박비에 대해 정산을 요청했습니다.', FALSE),
       (2, 'room_reserved', '숙소 예약 완료', '9월 1일 씨마크 호텔 예약이 완료되었습니다.', TRUE),
       (1, 'transport_reserved', '교통편 예약 완료', '8월 1일 서울-부산 KTX 예약이 완료되었습니다.', FALSE),
       (2, 'settlement_request', '정산 요청', '지젤님이 강릉 맛집1 비용 정산을 요청했습니다.', TRUE);



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
-- 주민등록증 테스트용 데이터
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0004114000001', '카리나', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0101014000002', '윈터', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0010304000003', '지젤', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0210234000002', '닝닝', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);



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
    issued_date    DATE,
    expiry_date    DATE,
    issuing_agency VARCHAR(50),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);
-- 운전면허증 테스트용 데이터
-- 카리나 (경남 양산 → 울산남부경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency)
VALUES (NULL, '0004114000001', '201234567810', '1종 보통',
        '2020-06-15', '2030-06-15', '울산남부경찰서');

-- 윈터 (부산 해운대 → 부산해운대경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency)
VALUES (NULL, '0101014000002', '211198765421', '2종 소형',
        '2021-03-10', '2031-03-10', '부산해운대경찰서');

-- 지젤 (도쿄 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency)
VALUES (NULL, '0010304000003', '221011223309', '2종 보통',
        '2022-07-22', '2032-07-22', '서울성동경찰서');

-- 닝닝 (하얼빈 출신 → 서울 활동지 기준 서울성동경찰서)
INSERT INTO DRIVER_LICENSE (member_id, id_card_number, license_number, license_type,
                            issued_date, expiry_date, issuing_agency)
VALUES (NULL, '0210234000002', '230944556633', '원동기장치자전거',
        '2023-01-05', '2033-01-05', '서울성동경찰서');



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
# 계좌 테스트 데이터
-- 카리나
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (NULL, '카리나', '1234567890001', '1111', 'KB', 100000.00);

-- 윈터
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (NULL, '윈터', '1234567890002', '2222', 'KB', 100000.00);

-- 지젤
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (NULL, '지젤', '1234567890003', '3333', 'KB', 100000.00);

-- 닝닝
INSERT INTO ACCOUNT (member_id, name, account_number, account_password, bank_name, balance)
VALUES (NULL, '닝닝', '1234567890004', '4444', 'KB', 100000.00);



-- ========================================================================================
-- 결제내역 테이블
-- ========================================================================================
CREATE TABLE PAYMENT_RECORD
(
    record_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT         NOT NULL,
    member_id        BIGINT         NOT NULL,
    payment_name     VARCHAR(100)   NOT NULL,
    payment_price    DECIMAL(15, 2) NOT NULL,
    payment_date     DATETIME       NOT NULL,
    payment_location VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES ACCOUNT (account_id),
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);
-- 결제내역 테스트용 데이터
-- 카리나 (member_id=1)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (1, 1, '편의점 간식', 4500.00, '2025-08-01 09:10:00', 'BUSAN'),
       (1, 1, '부산 맛집1', 51000.00, '2025-08-02 12:00:00', 'BUSAN'),
       (1, 1, '카페 커피', 6500.00, '2025-08-03 15:40:00', 'BUSAN');

-- 윈터 (member_id=2)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (2, 2, '주유소 결제', 54000.00, '2025-09-01 10:20:00', 'GANGNEUNG'),
       (2, 2, '숙박 비용', 193000.00, '2025-09-01 12:00:00', 'GANGNEUNG'),
       (2, 2, '교통비', 105000.00, '2025-09-01 13:30:00', 'GANGNEUNG'),
       (2, 2, '길거리 간식', 9999.00, '2025-09-02 17:30:00', 'GANGNEUNG'),
       (2, 2, '편의점 물품', 12000.00, '2025-09-03 22:10:00', 'GANGNEUNG');

-- 지젤 (member_id=3)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (3, 3, '강릉 맛집1', 61000.00, '2025-09-01 19:00:00', 'GANGNEUNG'),
       (3, 3, '강릉 맛집2', 10000.00, '2025-09-01 20:00:00', 'GANGNEUNG'),
       (3, 3, '기념품 구매', 22000.00, '2025-09-02 11:15:00', 'GANGNEUNG'),
       (3, 3, '카페 디저트', 8500.00, '2025-09-02 15:45:00', 'GANGNEUNG');

-- 닝닝 (member_id=4)
INSERT INTO PAYMENT_RECORD (account_id, member_id, payment_name, payment_price, payment_date, payment_location)
VALUES (4, 4, '강릉 맛집3', 10001.00, '2025-09-01 21:00:00', 'GANGNEUNG'),
       (4, 4, '편의점 간식', 5500.00, '2025-09-02 12:20:00', 'GANGNEUNG'),
       (4, 4, '기념품', 17000.00, '2025-09-02 16:30:00', 'GANGNEUNG');



-- ========================================================================================
-- 여행 테이블
-- ========================================================================================
CREATE TABLE TRIP
(
    trip_id       BIGINT AUTO_INCREMENT PRIMARY KEY,                               -- 여행 ID (기본키)
    member_id     BIGINT                              NOT NULL,                    -- 생성자 ID (외래키)
    trip_name     VARCHAR(255),                                                    -- 여행 이름 (nullable)
    trip_location ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,                    -- 여행 지역 (ENUM)
    start_date    DATE                                NOT NULL,                    -- 시작일
    end_date      DATE                                NOT NULL,                    -- 종료일
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 생성일시
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)                          -- MEMBER 테이블의 PK 참조
);
-- 여행 테스트용 데이터
INSERT INTO TRIP (trip_id, member_id, trip_name, trip_location, start_date, end_date)
VALUES (1, 1, '부산 여행', 'BUSAN', '2025-08-01', '2025-08-03'),
       (2, 2, '강릉 여행', 'GANGNEUNG', '2025-09-01', '2025-09-03'),
       (3, 4, '제주도 여행', 'JEJU', '2025-10-01', '2025-10-04'),
       (4, 4, '제주도 여행', 'JEJU', '2025-06-01', '2025-06-04'),
       (5, 3, '제주도 여행', 'JEJU', '2025-07-01', '2025-07-04'),
       (6, 4, '제주도 여행', 'JEJU', '2025-07-23', '2025-07-26'),
       (7, 2, '제주도 여행', 'JEJU', '2025-08-11', '2025-08-15'),
       (8, 4, '제주도 여행', 'JEJU', '2025-11-01', '2025-11-04'),
       (9, 1, '제주도 여행', 'JEJU', '2025-05-01', '2025-05-04'),
       (10, 4, '제주도 여행', 'JEJU', '2025-09-10', '2025-09-15');




-- ========================================================================================
-- 여행위치 테이블
-- ========================================================================================
CREATE TABLE TRIP_LOCATION
(
    location_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 위치 ID (기본키)
    location_name VARCHAR(100),                               -- 장소명
    latitude      DECIMAL(10, 8),                             -- 위도
    longitude     DECIMAL(11, 8),                             -- 경도
    address       VARCHAR(200)                                -- 주소
);
-- 여행위치 테스트용 데이터
INSERT INTO TRIP_LOCATION (location_name, latitude, longitude, address)
VALUES ('부산', 35.179554, 129.075642, '부산광역시 중구 중앙대로 100'),
       ('강릉', 37.751853, 128.876057, '강원특별자치도 강릉시 교동광장로 100'),
       ('제주도', 33.499621, 126.531188, '제주특별자치도 제주시 중앙로 100');



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
-- 여행 멤버 테스트용 데이터
INSERT INTO TRIP_MEMBER (trip_id, member_id, role, joined_at)
VALUES
-- 부산 여행: 카리나 혼자
(1, 1, 'HOST', NOW()),

-- 강릉 여행: 윈터 + 지젤 + 닝닝
(2, 2, 'HOST', NOW()),
(2, 3, 'MEMBER', NOW()),
(2, 4, 'MEMBER', NOW()),

-- 제주도 여행: 닝닝 혼자
(3, 4, 'HOST', NOW()),

(4, 4, 'HOST', NOW()),

(5, 3, 'HOST', NOW()),
(5, 4, 'MEMBER', NOW()),

(6, 4, 'HOST', NOW()),

(7, 2, 'HOST', NOW()),
(7, 4, 'MEMBER', NOW()),

(8, 4, 'HOST', NOW()),

(9, 1, 'HOST', NOW()),
(9, 4, 'MEMBER', NOW()),

(10, 4, 'HOST', NOW());



-- ========================================================================================
-- 여행날짜 테이블
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
-- 여행날짜 테스트용 데이터
INSERT INTO TRIP_DAY (trip_day_id, trip_id, day)
VALUES
-- 부산 여행 (3일)
(1, 1, '2025-08-01'),
(2, 1, '2025-08-02'),
(3, 1, '2025-08-03'),

-- 강릉 여행 (3일)
(4, 2, '2025-09-01'),
(5, 2, '2025-09-02'),
(6, 2, '2025-09-03'),

-- 제주도 여행 (4일)
(7, 3, '2025-10-01'),
(8, 3, '2025-10-02'),
(9, 3, '2025-10-03'),
(10, 3, '2025-10-04');



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
-- 예약 테스트용 데이터
INSERT INTO RESERVATION (reservation_id, trip_day_id, res_kind)
VALUES
-- trip_day_id: 1, 2, 3 => 카리나 부산 여행
(1, 1, 'ACCOMMODATION'), -- 숙박
(4, 2, 'RESTAURANT'), -- 식당
(5, 3, 'RESTAURANT'), -- 식당
(13, 1, 'TRANSPORT'), -- 교통
(14, 1, 'RESTAURANT'), -- 식당
-- trip_day_id: 4, 5, 6 => 윈터, 지젤, 닝닝 강릉 여행
(2, 4, 'ACCOMMODATION'), -- 숙박
(6, 5, 'RESTAURANT'), -- 식당
(7, 5, 'RESTAURANT'), -- 식당
(8, 6, 'RESTAURANT'), -- 식당
(15, 4, 'TRANSPORT'), -- 교통
(19, 4, 'TRANSPORT'), -- 교통
(20, 4, 'TRANSPORT'), -- 교통
(16, 4, 'RESTAURANT'), -- 식당
-- trip_day_id: 7, 8, 9, 10 => 닝닝 제주도 여행
(3, 7, 'ACCOMMODATION'), -- 숙박
(9, 8, 'RESTAURANT'), -- 식당
(10, 9, 'RESTAURANT'), -- 식당
(11, 9, 'RESTAURANT'), -- 식당
(12, 10, 'RESTAURANT'), -- 식당
(17, 7, 'TRANSPORT'), -- 교통
(18, 7, 'RESTAURANT'); -- 식당



-- ========================================================================================
-- 숙박정보 테이블
-- ========================================================================================
CREATE TABLE ACCOMMODATION_INFO
(
    accom_id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,               -- 숙박 ID (기본키)
    hotel_name      VARCHAR(255) NOT NULL,                                          -- 호텔 이름
    address         VARCHAR(255) NOT NULL,                                          -- 위치 (주소)
    latitude        DECIMAL(10, 7),                                                 -- 위도
    longitude       DECIMAL(10, 7),                                                 -- 경도
    description     TEXT,                                                           -- 설명
    hotel_image_url VARCHAR(255),                                                   -- 호텔 이미지 URL
    check_in_time   TIME         NOT NULL,                                          -- 체크인 시각
    check_out_time  TIME         NOT NULL,                                          -- 체크아웃 시각
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                            -- 생성일시
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 수정일시
);
-- 숙박정보 테스트용 데이터
INSERT INTO ACCOMMODATION_INFO (accom_id, hotel_name, address, latitude, longitude, description, hotel_image_url,
                                check_in_time, check_out_time)
VALUES (1, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 35.1664, 129.0624,
        '중심 업무 지구의 고층 유리 건물에 자리한 이 고급 호텔은 서면역에서 도보 5분, 광안리 해수욕장에서 지하철로 33분 거리에 있습니다. \n\n아늑하고 우아한 객실에 무료 Wi-Fi, 평면 TV, 차 및 커피 메이커가 갖춰져 있습니다. 스위트룸에는 거실이 추가되며 업그레이드 스위트룸에는 사우나, 벽난로, 식탁이 마련되어 있습니다. 클럽층 객실에는 무료 조식, 스낵, 애프터눈 티가 제공됩니다. 야구를 테마로 꾸민 스위트룸이 2곳 있습니다. 룸 서비스도 이용 가능합니다. \n\n레스토랑 5곳, 베이커리, 정기 라이브 음악 공연이 열리는 바가 있습니다. 헬스장, 사우나, 골프 연습장, 실내외 수영장도 이용할 수 있습니다.',
        'https://lh4.googleusercontent.com/proxy/XeXolS_HJuyW-TisSGI8fW3kC3jyFi1dzO--V8yWl6s28hPAzzPCFu-3c0pQrZRQWkcTXuSaDu-Z4aGfEaDpl5BwYEpt6jGkH_Z6JPOBpoZm5zBvek04T79JIRjKR4YxqCEy-BeI8cwZuFUKl71bgFW-E_0C2-E=w253-h168-k-no',
        '15:00:00', '11:00:00'),
       (2, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 37.7760, 128.9101,
        '해변가의 우아한 타워에 자리 잡고 있어 동해 바다가 바로 보이는 이 세련된 호텔은 정동진 기차역에서 6km 떨어져 있습니다.\n\n쾌적한 객실에는 평면 TV, Wi-Fi, 미니 냉장고, 유리 벽으로 된 욕실이 있으며, 대부분의 객실에서 바다 전망이 보입니다. 미니멀리즘 인테리어가 돋보이는 온돌 방식의 객실에는 이불이 제공됩니다. 품격 있는 스위트룸에는 휴식 공간이 추가되고, 업그레이드 스위트룸에는 식사 공간, 우아한 거실, 단독형 욕조를 구비한 고급 욕실이 있습니다. 룸서비스도 이용 가능합니다.\n\n세련된 레스토랑 2곳, 바다 전망이 보이는 바, 야외 인피니티 풀은 물론 실내 수영장, 헬스장, 어린이 놀이 공간과 현대적인 야외 원형 극장도 있습니다.',
        'https://lh3.googleusercontent.com/gps-proxy/ALd4DhGVp7wJ8RrkzR00agDAmjFjDNiEgLo2TqOFUb39rRFdsiWFsdbbyG_WGp6HpRDfn57rK7DL5M29YIQAyghX0sTSdn6CSrdIHTvPcVdAi1gJCFP6D-LOiejXDr_piBcIfwYm2FFJq4-lFWFuuEcdrwefVAMJ0MhwnW6js9H4KkN4prEn7mIDHr-B_g=w408-h301-k-no',
        '15:00:00', '11:00:00'),
       (3, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 33.2343, 126.5347,
        '제주의 바다를 마주한 JW 메리어트 제주 리조트 & 스파는 제주 국제공항에서 50분 거리에 위치하고 있습니다. 서귀포 매일올레시장과 산방산, 성산일출봉 등 자연경관 가까이 자리 잡은 JW 메리어트 제주에서 진정한 휴식을 즐겨보세요. 올데이 다이닝 레스토랑 아일랜드 키친에서 브런치 로열과 함께 여유롭게 하루를 시작하고, 더 라운지에서 애프터눈티 세트를 경험하실 수 있습니다.\n\n더 플라잉 호그에서는 우드 파이어 그릴에 구워 낸 제주식 구이 요리를 파인 다이닝 스타일로 추천해 드립니다. SPA by JW에서 페이셜 및 딥 티슈 마사지를 경험하며 웰니스에 집중해 보는 건 어떨까요?\n\n인피니티 풀을 포함해 총 4곳의 실내 수영장 또는 실외 수영장 또한 마련되어 있습니다. 패밀리클럽에서 아이들과 즐거운 시간을 보낼 수 있고, 어린이들을 위한 다양한 키즈 액티비티 프로그램도 준비됩니다. 완벽한 비즈니스 행사와 데스티네이션 웨딩을 계획하신다면, 한식 또는 양식 옵션을 선택하실 수 있는 맞춤 케이터링 메뉴가 제공되는 JW 메리어트 제주의 실내 혹은 실외 이벤트 공간을 활용해 보세요.\n\nLED TV, 미니바, 대리석 욕조 그리고 무료 Wi-Fi가 제공되는 안락한 객실에서 충분한 휴식을 취하세요. 대부분의 객실에 아름다운 오션뷰를 만끽할 수 있는 발코니가 설치되어 있습니다. JW 메리어트 제주에서 숨이 멎을 정도로 아름다운 제주도의 풍경을 경험해보세요.',
        'https://lh3.googleusercontent.com/gps-cs-s/AC9h4nqjKw1bpUenXlcqmUWTdbuOpiH_IvxLURkWSmrZMHtBlzpVwpi_PYH60bnsZTsBb7k6bR8xWlPzo5yaVYn_0DjvcTavpXNJNit3An3XU9LmqRP5vQc-MDkJKzL2g34jMbVN7k8=w253-h189-k-no',
        '15:00:00', '11:00:00');



-- ========================================================================================
-- 객실정보 테이블
-- ========================================================================================
CREATE TABLE ROOM_INFO
(
    room_id        BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,              -- 객실 ID (기본키)
    accom_id       BIGINT         NOT NULL,                                         -- 숙박 ID (외래키)
    name           VARCHAR(100)   NOT NULL,                                         -- 객실 이름
    price          DECIMAL(10, 2) NOT NULL,                                         -- 가격
    room_type      VARCHAR(50)    NOT NULL,                                         -- 객실 타입
    guests         INT            NOT NULL,                                         -- 인원 수
    room_image_url VARCHAR(255),                                                    -- 객실 이미지 URL
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    CONSTRAINT fk_room_info_accom_id
        FOREIGN KEY (accom_id) REFERENCES ACCOMMODATION_INFO (accom_id)
            ON DELETE CASCADE
);
-- 객실정보 테스트용 데이터
INSERT INTO ROOM_INFO (room_id, accom_id, name, price, room_type, guests, room_image_url)
VALUES (1, 1, '디럭스룸', 230000, 'deluxe', 2,
        'https://img.lottehotel.com/cms/asset/2025/01/31/3703/181026-2-2000-roo-LTSE%20(1).webp'),
       (2, 1, '트윈룸', 193000, 'twin', 2, 'https://www.associa.com/kor/syh/images/stay/twin_moderate.jpg'),
       (3, 2, '트윈룸', 193000, 'twin', 2, 'https://www.associa.com/kor/syh/images/stay/twin_moderate.jpg'),
       (4, 2, '패밀리룸', 340000, 'family', 3,
        'https://lh3.googleusercontent.com/proxy/4WVBk3sTn5uwS7kFDP3Sgl9-tKEvmyz06BuXFzI2XeORJwrKmJqPL7SkiDb7OEBdvTq-wFhD2xO0xCnBqQ9U3fIfRva_TN234qmyAACLBKzqR5-GHpx5i_L-GrMXYyI18d-xwAJRqtrKaoM5FHwPuiijDn-r-5AH'),
       (5, 3, '디럭스룸', 230000, 'deluxe', 2,
        'https://img.lottehotel.com/cms/asset/2025/01/31/3703/181026-2-2000-roo-LTSE%20(1).webp'),
       (6, 3, '패밀리룸', 340000, 'family', 3,
        'https://lh3.googleusercontent.com/proxy/4WVBk3sTn5uwS7kFDP3Sgl9-tKEvmyz06BuXFzI2XeORJwrKmJqPL7SkiDb7OEBdvTq-wFhD2xO0xCnBqQ9U3fIfRva_TN234qmyAACLBKzqR5-GHpx5i_L-GrMXYyI18d-xwAJRqtrKaoM5FHwPuiijDn-r-5AH');



-- ========================================================================================
-- 숙박예약 테이블
-- ========================================================================================
CREATE TABLE ACCOM_RES
(
    accom_res_id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,                -- 숙박 예약 ID (PK)
    room_id        BIGINT       NOT NULL,                                           -- 객실 ID (FK)
    accom_id       BIGINT       NOT NULL,                                           -- 숙박 ID (FK)
    reservation_id BIGINT       NULL,                                           -- 예약 ID (FK)
    trip_day_id    BIGINT       NULL,                                           -- 여행 날짜 ID (FK)
    hotel_name     VARCHAR(255) NOT NULL,                                           -- 업체명
    address        VARCHAR(255) NOT NULL,                                           -- 주소
    room_type      VARCHAR(50)  NOT NULL,                                           -- 객실 타입
    checkin_time   DATETIME     NOT NULL,                                           -- 체크인 시간
    checkout_time  DATETIME     NOT NULL,                                           -- 체크아웃 시간
    res_num        INT          NOT NULL,                                           -- 예약 인원
    is_checked     BOOLEAN      NOT NULL,                                           -- 체크인 여부
    user_name      VARCHAR(20)  NULL,                                               -- 예약자 이름
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    FOREIGN KEY (room_id) REFERENCES ROOM_INFO (room_id),
    FOREIGN KEY (accom_id) REFERENCES ACCOMMODATION_INFO (accom_id),
    FOREIGN KEY (reservation_id) REFERENCES RESERVATION (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY (trip_day_id)
);
-- 숙박예약 테스트용 데이터
INSERT INTO ACCOM_RES (accom_res_id, room_id, accom_id, reservation_id, trip_day_id, hotel_name, address, room_type,
                       checkin_time, checkout_time, res_num, is_checked, user_name)
VALUES (1, 1, 1, 1, 1, '부산 롯데 호텔', '부산광역시 부산진구 가야대로 772', 'twin', '2025-08-01 15:00:00', '2025-08-02 11:00:00', 1,
        false, '카리나'),
       (2, 4, 2, 2, 4, '씨마크 호텔', '강원특별자치도 강릉시 해안로406번길 2', 'family', '2025-09-01 15:00:00', '2025-09-02 11:00:00', 2,
        false, '윈터'),
       (3, 3, 3, 3, 7, 'JW 메리어트 제주 리조트 앤 스파', '제주특별자치도 서귀포시 호근동 399', 'deluxe', '2025-10-01 15:00:00',
        '2025-10-02 11:00:00', 1, false, '닝닝');



-- ========================================================================================
-- 교통정보 테이블
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
    train_no       VARCHAR(10)                                         NOT NULL,                            -- 열차 번호
    seat_type      ENUM ('general', 'first_class', 'silent', 'family') NOT NULL,                            -- 좌석 종류
    seat_total     INT                                                 NOT NULL,                            -- 총 좌석 수
    seat_remain    INT                                                 NOT NULL,                            -- 남은 좌석 수
    price          DECIMAL(15, 2)                                      NOT NULL,                            -- 가격
    is_visible     BOOLEAN                                             NOT NULL                             -- 표시 여부
);
-- 교통정보 테스트용 데이터
INSERT INTO TRANSPORT_INFO (transport_id, departure_id, arrival_id, departure_time, arrival_time, train_type, train_no,
                            seat_type, seat_total, seat_remain, price, is_visible)
VALUES (1, 'SEOUL', 'BUSAN', '2025-08-01 09:00', '2025-08-01 12:00', 'ktx', 'KTX123', 'general', 100, 90, 54000, true),
       (2, 'SEOUL', 'GANGNEUNG', '2025-09-01 08:00', '2025-09-01 11:00', 'ktx', 'KTX456', 'general', 100, 98, 35000,
        true),
       (3, 'SEOUL', 'JEJU', '2025-10-01 07:30', '2025-10-01 09:30', 'ktx-eum', 'KTX789', 'general', 100, 99, 48000,
        true);



-- ========================================================================================
-- 교통예약 테이블
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
-- 교통예약 테스트용 데이터
INSERT INTO TRAN_RES (tran_res_id, transport_id, reservation_id, trip_day_id, departure, arrival, seat_room_no, seat_number, seat_type,
                      booked_at, price)
VALUES (1, 1, 13, 1, '서울역', '부산역', 3, 'A1', 'general', NOW(), 54000),
       (2, 2, 15, 4, '서울역', '강릉역', 1, 'B2', 'general', NOW(), 35000),
       (3, 2, 19, 4, '서울역', '강릉역', 1, 'B3', 'general', NOW(), 35000),
       (4, 2, 20, 4, '서울역', '강릉역', 1, 'C1', 'general', NOW(), 35000),
       (5, 3, 17, 7, '서울', '제주', 2, 'C1', 'general', NOW(), 48000);
-- 기차인거 생각 못하고 제주도 해버렸는데 테스트용이니까 봐주세유 ㅎ



-- ========================================================================================
-- 식당정보 테이블
-- ========================================================================================
CREATE TABLE RESTAURANT_INFO
(
    rest_id        BIGINT                                                                        NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 식당 ID (PK)
    rest_name      VARCHAR(255)                                                                  NOT NULL,                            -- 식당 이름
    address        VARCHAR(255)                                                                  NOT NULL,                            -- 주소
    category       ENUM ('korean', 'chinese', 'japanese', 'western', 'etc')                      NOT NULL,                            -- 카테고리
    rest_image_url VARCHAR(255)                                                                  NOT NULL,                            -- 식당 이미지
    res_time_list  ENUM ('12:00', '12:30', '13:00', '13:30', '18:00', '18:30', '19:00', '20:00') NOT NULL,                            -- 예약 시간 목록
    phone          VARCHAR(20)                                                                   NULL,                                -- 전화번호
    description    TEXT                                                                          NULL,                                -- 설명
    latitude       DECIMAL(10, 7)                                                                NULL,                                -- 위도
    longitude      DECIMAL(10, 7)                                                                NULL,                                -- 경도
    breaktime      VARCHAR(255)                                                                  NULL,                                -- 브레이크타임
    closed_days    VARCHAR(255)                                                                  NULL,                                -- 휴무일
    menu_url       VARCHAR(255)                                                                  NULL,                                -- 메뉴 URL
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                                                                               -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP                                                    -- 수정일시
);
-- 식당정보 테스트용 데이터
INSERT INTO RESTAURANT_INFO (rest_id, rest_name, address, category, rest_image_url, res_time_list,
                             phone, latitude, longitude, closed_days, menu_url)
VALUES (1, '해운대 소문난 횟집', '부산광역시 해운대구 달맞이길 123', 'korean',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '18:00',
        '051-123-4567', 35.1634, 129.1636, '월요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (2, '명동 마라탕', '서울특별시 중구 명동10길 7', 'chinese',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '19:00',
        '02-321-7654', 37.5636, 126.982, '화요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (3, '홍대 초밥선생', '서울특별시 마포구 홍익로5길 20', 'japanese',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '13:00',
        '02-555-1234', 37.5563, 126.9221, '수요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (4, '이태원 스테이크하우스', '서울특별시 용산구 이태원로 123', 'western',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '20:00',
        '02-777-8888', 37.5342, 126.9946, '월요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (5, '강릉 바닷가 포장마차', '강원도 강릉시 경포로 89', 'etc',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '18:30',
        NULL, 37.7998, 128.9024, NULL, NULL),
       (6, '광안리 라멘집', '부산광역시 수영구 광안해변로 200', 'japanese',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '12:30',
        '051-888-7777', 35.1532, 129.1183, '화요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (7, '광안리 불백집', '부산광역시 수영구 광안해변로 55', 'korean',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '12:00',
        '051-112-3344', 35.1531, 129.1181, '월요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (8, '서면 중화반점', '부산광역시 부산진구 서면로 12', 'chinese',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '18:30',
        '051-567-8901', 35.1576, 129.0592, '화요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (9, '경포해변 회센터', '강원도 강릉시 해안로 170', 'korean',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '19:00',
        '033-111-2233', 37.7951, 128.9154, '수요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (10, '강릉 돈가스하우스', '강원도 강릉시 성덕포남로 98', 'western',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '13:30',
        '033-999-8888', 37.7709, 128.9072, '목요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (11, '제주 흑돼지촌', '제주특별자치도 제주시 중앙로 220', 'korean',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '18:00',
        '064-223-4455', 33.5006, 126.5312, '화요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (12, '서귀포 해녀의 집', '제주특별자치도 서귀포시 중문관광로 213', 'etc',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '12:30',
        '064-334-7788', 33.2491, 126.4129, '일요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (13, '제주시 갈치조림골목', '제주특별자치도 제주시 삼성로 45', 'korean',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '13:00',
        '064-567-1234', 33.5112, 126.5203, '수요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (14, '중문 랍스터 뷔페', '제주특별자치도 서귀포시 중문로 77', 'western',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '19:00',
        '064-777-9090', 33.2478, 126.4075, '월요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg'),
       (15, '제주 오마카세 스시', '제주특별자치도 제주시 연북로 201', 'japanese',
        'https://postfiles.pstatic.net/MjAyNTAyMTBfMjgz/MDAxNzM5MTg2MTQ4Njg3.m3W24gJj0DheNFhlMC30cgPdgTZ4C2vYdxk_Wqzju84g.pRaFhAQXTyLTuix0GzG5R1Nrv8_g8_hpXfCFoxmPqZcg.JPEG/1000037063.jpg?type=w966',
        '18:30',
        '064-444-8888', 33.4956, 126.5322, '화요일',
        'https://m.health.chosun.com/site/data/img_dir/2024/02/29/2024022901106_0.jpg');



-- ========================================================================================
-- 식당예약 테이블
-- ========================================================================================
CREATE TABLE REST_RES
(
    rest_res_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,                -- 식당 예약 ID (PK)
    rest_id        BIGINT       NOT NULL,                                           -- 식당 ID (FK)
    reservation_id BIGINT       NULL,                                           -- 예약 ID (FK)
    trip_day_id    BIGINT       NULL,                                           -- 여행 날짜 ID (FK)
    rest_name      VARCHAR(255) NOT NULL,                                           -- 식당 이름
    address        VARCHAR(255) NOT NULL,                                           -- 주소
    res_num        INT          NOT NULL,                                           -- 예약 인원
    res_date       DATE     NOT NULL,       -- 예약일
    res_time       TIME         NOT NULL,   -- 예약 시간
    category       ENUM ('korean', 'chinese', 'japanese', 'western', 'etc') NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 생성일시
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    FOREIGN KEY (rest_id) REFERENCES RESTAURANT_INFO (rest_id),
    FOREIGN KEY (reservation_id) REFERENCES RESERVATION (reservation_id),
    FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY (trip_day_id)
);
-- 식당예약 테스트용 데이터
INSERT INTO REST_RES (rest_res_id, rest_id, reservation_id, trip_day_id, rest_name, address, res_num, res_date, res_time)
VALUES
-- 카리나 부산 여행
(1, 1, 14, 1, '해운대 소문난 횟집', '부산광역시 해운대구 달맞이길 123', 1, '2025-08-01', '18:00'),
(2, 6, 4, 2, '광안리 라멘집', '부산광역시 수영구 광안해변로 200', 1, '2025-08-02', '12:00'),
(3, 7, 5, 3, '광안리 불백집', '부산광역시 수영구 광안해변로 55', 1, '2025-08-03', '12:30'),

-- 윈터, 지젤, 닝닝 강릉 여행
(4, 5, 16, 4, '강릉 바닷가 포장마차', '강원도 강릉시 경포로 89', 3, '2025-09-01', '18:00'),
(5, 9, 6, 5, '경포해변 회센터', '강원도 강릉시 해안로 170', 3, '2025-09-02', '12:30'),
(6, 10, 7, 5, '강릉 돈가스하우스', '강원도 강릉시 성덕포남로 98', 3, '2025-09-02', '18:30'),
(7, 5, 8, 6, '강릉 바닷가 포장마차', '강원도 강릉시 경포로 89', 3, '2025-09-03', '12:00'),

-- 닝닝 제주도 여행
(8, 11, 18, 7, '제주 흑돼지촌', '제주특별자치도 제주시 중앙로 220', 1, '2025-10-01', '18:30'),
(9, 12, 9, 8, '서귀포 해녀의 집', '제주특별자치도 서귀포시 중문관광로 213', 1, '2025-10-02', '12:30'),
(10, 13, 10, 9, '제주시 갈치조림골목', '제주특별자치도 제주시 삼성로 45', 1, '2025-10-03', '12:00'),
(11, 14, 11, 9, '중문 랍스터 뷔페', '제주특별자치도 서귀포시 중문로 77', 1, '2025-10-03', '17:30'),
(12, 15, 12, 10, '제주 오마카세 스시', '제주특별자치도 제주시 연북로 201', 1, '2025-10-04', '13:00');



-- ========================================================================================
-- 비용 테이블
-- ========================================================================================
CREATE TABLE EXPENSE
(
    expense_id           BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,                            -- 비용 ID (기본키)
    trip_id              BIGINT                              NOT NULL,                                                       -- 여행 ID (외래키)
    member_id            BIGINT                              NOT NULL,                                                       -- 결제자 ID (외래키)
    expense_name         VARCHAR(100)                        NOT NULL,                                                       -- 결제명
    amount               DECIMAL(15, 2)                      NOT NULL,                                                       -- 금액 (소수점 2자리)
    location             ENUM ('BUSAN', 'GANGNEUNG', 'JEJU') NOT NULL,                                                       -- 결제 위치
    settlement_completed BOOLEAN                             NOT NULL,                                                       -- 정산 완료 여부
    created_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 작성일시
    updated_at           TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id)                                                                          -- 여행 ID 외래키
        ON DELETE CASCADE
);
-- 비용 테스트용 데이터
INSERT INTO EXPENSE (trip_id, member_id, expense_name, amount, location, settlement_completed)
VALUES (2, 2, '숙박 비용', 193000, 'GANGNEUNG', false),
       (2, 2, '교통비', 105000, 'GANGNEUNG', false),
       (2, 3, '강릉 맛집1', 61000, 'GANGNEUNG', false),
       (2, 2, '길거리 간식', 9999, 'GANGNEUNG', false),  -- 정확히 나눠 떨어지는 경우
       (2, 3, '강릉 맛집2', 10000, 'GANGNEUNG', false), -- 3333.33...으로 나머지가 1인 경우
       (2, 4, '강릉 맛집3', 10001, 'GANGNEUNG', false);
-- 3333.66...으로 나머지가 2인 경우


-- ========================================================================================
-- 정산내역 테이블
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
-- 정산내역 테스트용 데이터
INSERT INTO SETTLEMENT_NOTES (expense_id, trip_id, member_id, share_amount, is_payed, received)
VALUES
-- (2, 2, '숙박 비용', 193000, 'GANGNEUNG', false)에 대한 정산
(1, 2, 3, 64333, false, true),
(1, 2, 4, 64333, false, true),
(1, 2, 2, 64334, true, false),

-- (2, 2, '교통비', 105000, 'GANGNEUNG', false)에 대한 정산
(2, 2, 3, 35000, false, true),
(2, 2, 4, 35000, false, true),
(2, 2, 2, 35000, true, false),

-- (2, 3, '강릉 맛집1', 61000, 'GANGNEUNG', false)에 대한 정산
(3, 2, 2, 20333, false, true),
(3, 2, 4, 20333, false, true),
(3, 2, 3, 20334, true, false),

-- (2, 2, '길거리 간식', 9999, 'GANGNEUNG', false)에 대한 정산
(4, 2, 3, 3333, false, true),
(4, 2, 4, 3333, false, true),
(4, 2, 2, 3333, true, false),

-- (2, 3, '강릉 맛집2', 10000, 'GANGNEUNG', false)에 대한 정산
(5, 2, 2, 3333, false, true),
(5, 2, 4, 3333, false, true),
(5, 2, 3, 3334, true, false),

-- (2, 4, '강릉 맛집3', 10001, 'GANGNEUNG', false)에 대한 정산
(6, 2, 2, 3333, false, true),
(6, 2, 3, 3333, false, true),
(6, 2, 4, 3335, true, false);



-- ========================================================================================
-- 여행기록 테이블
-- ========================================================================================
CREATE TABLE TRIP_RECORDS
(
    record_id   BIGINT AUTO_INCREMENT PRIMARY KEY,                                           -- 기록 ID (PK)
    trip_id     BIGINT       NOT NULL,                                                       -- 여행 ID (FK)
    title       VARCHAR(255) NOT NULL,                                                       -- 제목
    record_date DATE         NOT NULL,                                                       -- 날짜
    content     TEXT,                                                                        -- 내용
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 작성일시
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정일시

    -- 여행 삭제되면 해당 여행기록도 같이 삭제
    FOREIGN KEY (trip_id) REFERENCES TRIP (trip_id) ON DELETE CASCADE
);
-- 여행기록 테스트용 데이터
INSERT INTO TRIP_RECORDS (record_id, trip_id, title, record_date, content)
VALUES (1, 1, '부산 도착!', '2025-08-01', '부산역에서 해운대까지 고고'),
       (2, 1, '부산 이틀차', '2025-08-02', '오늘도 너무 재미있었다~!! 이재모 피자 먹고싶다~'),
       (3, 1, '부산 마지막 날', '2025-08-03', '매우 아쉽다!! 부산 또 오고 싶다!!!'),
       (4, 2, '강릉이다~~', '2025-09-01', '바다를 보며 여유로운 하루'),
       (5, 2, '강릉 이틀차', '2025-09-02', '윈터, 지젤, 닝닝의 강릉 여행~'),
       (6, 2, '강릉 마지막~', '2025-09-03',
        '재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~재밌었다~'),
       (7, 3, '제주도 도착~', '2025-10-01', '제주도 첫날은 역시 흑돼지'),
       (8, 3, '제주도 이틀차', '2025-10-02', '모아 팀이 제주도 갔으면~~ 기다려라 제주도'),
       (9, 3, '제주도 마지막~', '2025-10-04', '제주도 재밌었다. 이제 집으로 출발~');



-- ========================================================================================
-- 여행기록 사진 테이블
-- ========================================================================================
CREATE TABLE TRIP_RECORDS_IMAGES
(
    image_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT       NOT NULL,
    image_url VARCHAR(255) NOT NULL,

    CONSTRAINT fk_trip_record_image_record_id
        FOREIGN KEY (record_id)
            REFERENCES TRIP_RECORDS (record_id)
            ON DELETE CASCADE
);
-- 여행기록 사진 테스트용 데이터
INSERT INTO TRIP_RECORDS_IMAGES (record_id, image_url)
VALUES (1, 'https://www.visitbusan.net/uploadImgs/files/hqimgfiles/20200327141200390_thumbL'),
       (1, 'https://www.visitbusan.net/uploadImgs/files/hqimgfiles/20200326155100723_thumbL'),
       (2,
        'https://search.pstatic.net/common/?src=http%3A%2F%2Fblogfiles.naver.net%2F20151130_191%2Fhan4869_1448869350319oBHdK_JPEG%2FDSC03360.jpg&type=sc960_832'),
       (3,
        'https://search.pstatic.net/common/?src=http%3A%2F%2Fblogfiles.naver.net%2FMjAyNTAzMDFfNiAg%2FMDAxNzQwODE5ODg4OTA5.nyNgSTkQkiCKfJbYStzMSXR1mV6pH7ar8g1IZMzEArQg.icnISzjIpKLF7vMNbsE8W05FUADjgU_a8mtXegAyVdwg.JPEG%2FIMG_7016.jpg&type=sc960_832'),
       (4,
        'https://visitgangneung.net/dzSmart/upfiles/board/2024August/35/1724727673_611e7956d474fae19a9dd634d2b0ca4d.jpg'),
       (5,
        'https://visitgangneung.net/dzSmart/upfiles/board/2024August/35/1724727782_57a7ed8f07837ef97a3c93a2b8f07865.jpg'),
       (5,
        'https://visitgangneung.net/dzSmart/upfiles/board/2024August/35/1724727728_d8bdb1361ccd0641df5a1aeae105fe53.jpg'),
       (7, 'https://api.cdn.visitjeju.net/photomng/imgpath/202408/16/f305a4d6-4dc7-43e7-95f2-8a0f92a5b840.jpg'),
       (8, 'https://api.cdn.visitjeju.net/photomng/imgpath/202408/21/46ff3ceb-5a9a-4de1-a3b6-2b126fd5e16d.jpg'),
       (8, 'https://api.cdn.visitjeju.net/photomng/imgpath/202408/21/05fea01c-82c4-4fca-a083-0e04800d311f.jpg'),
       (8, 'https://api.cdn.visitjeju.net/photomng/imgpath/202408/21/0f727fc3-79a0-4c15-9c5e-0c97c776be35.jpg'),
       (9, 'https://api.cdn.visitjeju.net/photomng/imgpath/201811/06/2509be16-0b15-427f-baac-8a638e7e1da3.jpg'),
       (9, 'https://api.cdn.visitjeju.net/photomng/imgpath/201804/30/ab201f2c-8586-474e-aca8-a789c19e5544.jpg');
