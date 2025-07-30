package org.moa.reservation.transport.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.moa.global.response.ApiResponse;
import org.moa.global.type.StatusCode;
import org.moa.reservation.transport.dto.TransportInfoResponse;
import org.moa.reservation.transport.dto.TransportSeatsInfoResponse;
import org.moa.reservation.transport.service.TransportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member/reservation/transport")
@RequiredArgsConstructor
public class TransportController {

	private final TransportService transportService;

	@GetMapping("/seats")
	public ResponseEntity<ApiResponse<Map<Integer, List<TransportSeatsInfoResponse>>>> getSeas(
		@RequestParam Long transportId
	) {
		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(transportService.getSeats(transportId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<TransportInfoResponse>>> searchTransport(
		@PageableDefault(size = 10, sort = "transportId") Pageable pageable,
		@RequestParam String departureName,
		@RequestParam String destinationName,
		@RequestParam
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		LocalDateTime departureDateTime
	) {
		log.info("TransportController ==== searchTransport page={} sort={} from {} to {} at {}",
			pageable.getPageNumber(), pageable.getSort(),
			 departureName, destinationName, departureDateTime);

		Page<TransportInfoResponse> page = transportService.searchTransports(
			 departureName, destinationName, departureDateTime, pageable
		);

		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(page));
	}
}
