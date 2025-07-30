package org.moa.reservation.transport.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.reservation.transport.dto.TransportInfoResponse;

@Mapper
public interface TransportMapper {
	// 페이징 조회용: offset, limit
	List<TransportInfoResponse> selectTransports(
		@Param("departureName")     String        departureName,
		@Param("destinationName")   String        destinationName,
		@Param("departureDateTime") LocalDateTime departureDateTime,
		@Param("offset")            int           offset,
		@Param("limit")             int           limit
	);

	// 전체 건수 조회용
	int countTransports(
		@Param("departureName")     String        departureName,
		@Param("destinationName")   String        destinationName,
		@Param("departureDateTime") LocalDateTime departureDateTime
	);
}
