#기존 테이블 제거 (외래키 제약 해제 → 삭제 순서 주의)
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS PAYMENT_RECORD;
DROP TABLE IF EXISTS MEMBER_ACCOUNT;
DROP TABLE IF EXISTS DRIVER_LICENSE;
DROP TABLE IF EXISTS ID_CARD;
DROP TABLE IF EXISTS OWNER;
DROP TABLE IF EXISTS MEMBER;

SET FOREIGN_KEY_CHECKS = 1;

# MEMBER 테이블 생성
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

# ID_CARD 테이블 생성
CREATE TABLE ID_CARD
(
    id_card_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT UNIQUE,
    id_card_number CHAR(13)     NOT NULL UNIQUE,
    name           VARCHAR(20)  NOT NULL,
    issued_date    DATE         NOT NULL,
    address        VARCHAR(255) NOT NULL,
    image_url      VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT FK_MEMBER_TO_ID_CARD_1
        FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

# DRIVER_LICENSE 테이블 생성
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

# MEMBER_ACCOUNT 테이블 생성
CREATE TABLE MEMBER_ACCOUNT
(
    account_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id        BIGINT,
    account_number   VARCHAR(50)    NOT NULL UNIQUE,
    account_password VARCHAR(255)   NOT NULL,
    bank_name        ENUM ('KB')    NOT NULL,
    balance          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    is_active        BOOLEAN                 DEFAULT TRUE,
    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

# PAYMENT_RECORD 테이블 생성
CREATE TABLE PAYMENT_RECORD
(
    record_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT         NOT NULL,
    member_id        BIGINT         NOT NULL,
    payment_name     VARCHAR(100)   NOT NULL,
    payment_price    DECIMAL(15, 2) NOT NULL,
    payment_date     DATETIME       NOT NULL,
    payment_location VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES MEMBER_ACCOUNT (account_id),
    FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

# OWNER 테이블 생성
CREATE TABLE OWNER
(
    owner_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_password VARCHAR(255) NOT NULL,
    owner_no       VARCHAR(255) NOT NULL
);

# 주민등록증 테스트 데이터
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0004114000001', '카리나', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0101014000002', '윈터', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0010304000003', '지젤', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);
insert into ID_CARD(id_card_number, name, issued_date, address, image_url)
values ('0210234000002', '닝닝', '2020-01-01', '서울특별시 성동구 왕십리로 83-21', NULL);

# 운전면허증 테스트 데이터
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

# 계좌 테스트 데이터
-- 카리나
INSERT INTO MEMBER_ACCOUNT (member_id, account_number, account_password, bank_name, balance)
VALUES (NULL, '1234567890001', '1111', 'KB', 100000.00);

-- 윈터
INSERT INTO MEMBER_ACCOUNT (member_id, account_number, account_password, bank_name, balance)
VALUES (NULL, '1234567890002', '2222', 'KB', 100000.00);

-- 지젤
INSERT INTO MEMBER_ACCOUNT (member_id, account_number, account_password, bank_name, balance)
VALUES (NULL, '1234567890003', '3333', 'KB', 100000.00);

-- 닝닝
INSERT INTO MEMBER_ACCOUNT (member_id, account_number, account_password, bank_name, balance)
VALUES (NULL, '1234567890004', '4444', 'KB', 100000.00);