package org.moa.reservation.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

import org.moa.reservation.transport.dto.TransportInfoResponse;
import org.moa.reservation.transport.dto.TransportSeatsInfoResponse;
import org.moa.reservation.transport.mapper.TransportMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportServiceImpl implements TransportService {

	private final TransportMapper transportMapper;

	@Override
	public Map<Integer, List<TransportSeatsInfoResponse>> getSeats(Long transportId) {
		// 목록 조회
		List<TransportSeatsInfoResponse> transportSeatsInfos = transportMapper.selectSeatsByTransportId(transportId);

		// 2) seatRoomNo 기준으로 묶어서 LinkedHashMap 으로 반환
		return transportSeatsInfos.stream()
			.collect(Collectors.groupingBy(
				TransportSeatsInfoResponse::getSeatRoomNo,
				LinkedHashMap::new,       // insertion-order 유지
				Collectors.toList()
			));
	}

	@Override
	public Page<TransportInfoResponse> searchTransports(
		String departureName,
		String destinationName,
		LocalDateTime departureDateTime,
		Pageable pageable
	) {
		// 목록 조회
		List<TransportInfoResponse> transportInfos = transportMapper.selectTransports(
			departureName, destinationName, departureDateTime, pageable.getPageNumber() * pageable.getPageSize(), pageable.getPageSize()
		);

		//전체 건수 조회
		int total = transportMapper.countTransports(
			departureName, destinationName, departureDateTime
		);

		//Spring Data의 PageImpl로 감싸서 반환
		return new PageImpl<>(transportInfos, pageable, total);
	}
}
