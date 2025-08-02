package org.moa.reservation.transport.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.moa.global.response.ApiResponse;
import org.moa.global.security.domain.CustomUser;
import org.moa.global.type.StatusCode;
import org.moa.reservation.transport.dto.TransPaymentRequestDto;
import org.moa.reservation.transport.dto.TransResCancelRequestDto;
import org.moa.reservation.transport.dto.TranstInfoResponse;
import org.moa.reservation.transport.dto.TransResRequestDto;
import org.moa.reservation.transport.dto.TransSeatsInfoResponse;
import org.moa.reservation.transport.service.TransportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	public ResponseEntity<ApiResponse<Map<Integer, List<TransSeatsInfoResponse>>>> getSeas(
		@RequestParam Long transportId
	) {
		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(transportService.getSeats(transportId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<TranstInfoResponse>>> searchTransport(
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

		Page<TranstInfoResponse> page = transportService.searchTransports(
			 departureName, destinationName, departureDateTime, pageable
		);

		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(page));
	}

	@PostMapping("/seats")
	public ResponseEntity<ApiResponse<?>> reserveSeats(
		@RequestBody TransResRequestDto dto
	) {
		return ResponseEntity.status(StatusCode.OK.getStatus()).body(ApiResponse.of(transportService.reserveTransportSeats(dto)));
	}

	@PostMapping("/pay")
	public ResponseEntity<ApiResponse<?>> pay(
		@AuthenticationPrincipal CustomUser customUser,
		@RequestBody TransPaymentRequestDto dto
	) {
		Long memberId = customUser.getMember().getMemberId();

		return ResponseEntity
			.status(StatusCode.OK.getStatus())
			.body(ApiResponse.of(transportService.seatPayment(memberId, dto)));
	}

	@PostMapping("/cancel")
	public ResponseEntity<ApiResponse<?>> cancel(
		@AuthenticationPrincipal CustomUser customUser,
		@RequestBody @Valid TransResCancelRequestDto dto
	) {
		Long memberId = customUser.getMember().getMemberId();


		return ResponseEntity
			.status(StatusCode.OK.getStatus())
			.body(ApiResponse.of(transportService.cancelReservation(memberId, dto)));
	}
}
