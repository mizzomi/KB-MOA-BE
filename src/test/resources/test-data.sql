-- test-data.sql
-- 테스트 회원 데이터
INSERT INTO MEMBER (member_id, email, name) VALUES
                                                (1, 'user1@test.com', '테스트유저1'),
                                                (2, 'user2@test.com', '테스트유저2'),
                                                (3, 'user3@test.com', '테스트유저3');

-- 테스트 여행 데이터
INSERT INTO TRIP (trip_id, trip_name, start_date, end_date, created_by) VALUES
                                                                            (1, '제주도 여행', '2024-03-01', '2024-03-03', 1),
                                                                            (2, '부산 여행', '2024-03-15', '2024-03-17', 2);

-- 테스트 여행일정 데이터 (중요: TRIP_DAY 테이블)
INSERT INTO TRIP_DAY (trip_day_id, trip_id, day, day_date) VALUES
                                                               (1, 1, 1, '2024-03-01'),
                                                               (2, 1, 2, '2024-03-02'),
                                                               (3, 1, 3, '2024-03-03'),
                                                               (4, 2, 1, '2024-03-15'),
                                                               (5, 2, 2, '2024-03-16'),
                                                               (6, 2, 3, '2024-03-17');

-- 테스트 교통수단 데이터
INSERT INTO TRANSPORT (transport_id, transport_name, transport_type, total_seats, available_seats, price, departure_time, arrival_time) VALUES
                                                                                                                                            (1, '제주항공 123편', 'FLIGHT', 150, 10, 89000.00, '2024-03-01 09:00:00', '2024-03-01 10:30:00'),
                                                                                                                                            (2, 'KTX 101호', 'TRAIN', 400, 50, 45000.00, '2024-03-15 08:00:00', '2024-03-15 11:00:00'),
                                                                                                                                            (3, '고속버스 501번', 'BUS', 45, 5, 25000.00, '2024-03-01 14:00:00', '2024-03-01 18:00:00');
