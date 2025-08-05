-- schema.sql
-- 테이블 삭제 (재실행 시 오류 방지)
DROP TABLE IF EXISTS TRIP_DAY;
DROP TABLE IF EXISTS TRANSPORT;
DROP TABLE IF EXISTS TRIP;
DROP TABLE IF EXISTS MEMBER;

-- MEMBER 테이블
CREATE TABLE MEMBER (
                        member_id BIGINT PRIMARY KEY,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TRIP 테이블
CREATE TABLE TRIP (
                      trip_id BIGINT PRIMARY KEY,
                      trip_name VARCHAR(255) NOT NULL,
                      start_date DATE NOT NULL,
                      end_date DATE NOT NULL,
                      created_by BIGINT NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (created_by) REFERENCES MEMBER(member_id)
);

-- TRIP_DAY 테이블 (중요: 이 테이블이 누락되어 오류 발생)
CREATE TABLE TRIP_DAY (
                          trip_day_id BIGINT PRIMARY KEY,
                          trip_id BIGINT NOT NULL,
                          day INT NOT NULL,
                          day_date DATE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (trip_id) REFERENCES TRIP(trip_id)
);

-- TRANSPORT 테이블
CREATE TABLE TRANSPORT (
                           transport_id BIGINT PRIMARY KEY,
                           transport_name VARCHAR(255) NOT NULL,
                           transport_type VARCHAR(50) NOT NULL,
                           total_seats INT NOT NULL,
                           available_seats INT NOT NULL,
                           price DECIMAL(10,2) NOT NULL,
                           departure_time TIMESTAMP,
                           arrival_time TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 예약 관련 테이블들 (필요에 따라 추가)
CREATE TABLE RESERVATION (
                             reservation_id BIGINT PRIMARY KEY,
                             member_id BIGINT NOT NULL,
                             trip_day_id BIGINT NOT NULL,
                             transport_id BIGINT NOT NULL,
                             seat_count INT NOT NULL,
                             total_price DECIMAL(10,2) NOT NULL,
                             reservation_status VARCHAR(20) DEFAULT 'PENDING',
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (member_id) REFERENCES MEMBER(member_id),
                             FOREIGN KEY (trip_day_id) REFERENCES TRIP_DAY(trip_day_id),
                             FOREIGN KEY (transport_id) REFERENCES TRANSPORT(transport_id)
);
