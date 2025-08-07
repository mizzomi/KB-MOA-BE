package org.moa.member.service;

import org.moa.global.account.mapper.AccountMapper;
import org.moa.global.handler.BusinessException;
import org.moa.global.notification.dto.FcmTokenRequestDto;
import org.moa.global.type.StatusCode;
import org.moa.global.util.HashUtil;
import org.moa.member.dto.join.MemberJoinRequestDto;
import org.moa.member.dto.verify.MemberVerifyRequestDto;
import org.moa.member.entity.Member;
import org.moa.member.mapper.DriverLicenseMapper;
import org.moa.member.mapper.IdCardMapper;
import org.moa.member.mapper.MemberMapper;
import org.moa.trip.dto.trip.TripMemberResponseDto;
import org.moa.trip.entity.Trip;
import org.moa.trip.entity.TripMember;
import org.moa.trip.mapper.TripMapper;
import org.moa.trip.mapper.TripMemberMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	private final MemberMapper memberMapper;
	private final TripMemberMapper tripMemberMapper;
	private final IdCardMapper idCardMapper;
	private final AccountMapper accountMapper;
	private final DriverLicenseMapper driverLicenseMapper;

	// @Override
	// public boolean checkDuplicate(String email) {
	// 	Member member = memberMapper.get(email);
	// 	return member != null;
	// }
	//
	// @Override
	// public Member get(String email) {
	// 	Member member = Optional.ofNullable(memberMapper.get(email))
	// 		.orElseThrow(() -> new NoSuchElementException("해당 이메일의 사용자가 없습니다."));
	// 	return member;
	// }
	@Override
	@Transactional
	public boolean updateFcmToken(FcmTokenRequestDto dto){
		// 1. 현재 로그인된 사용자 정보 조회
		UserDetails userDetails = null;
		try {
			Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			if (principal instanceof UserDetails) {
				userDetails = (UserDetails) principal;
			} else {
				log.warn("인증된 사용자 정보가 UserDetails 타입이 아닙니다.");
				throw new BusinessException(StatusCode.UNAUTHORIZED);
			}
		} catch (Exception e) {
			log.error("인증된 사용자 정보를 가져오는 데 실패했습니다.", e);
			throw new BusinessException(StatusCode.UNAUTHORIZED);
		}

		// 2. Member 정보 조회
		Member member;
		try {
			member = memberMapper.getByEmail(userDetails.getUsername());
			if (member == null) {
				log.warn("사용자 이메일({})에 해당하는 회원 정보를 찾을 수 없습니다.", userDetails.getUsername());
				throw new BusinessException(StatusCode.BAD_REQUEST, "해당하는 사용자 이메일의 유저가 없습니다.");
			}
		} catch (DataAccessException e) {
			log.error("회원 정보 조회 중 데이터베이스 오류 발생", e);
			throw new BusinessException(StatusCode.INTERNAL_ERROR, "회원 정보 조회 중 오류가 발생했습니다.");
		}
		log.info("member : {} ",member);

		try{
			int result = memberMapper.updateFcmToken(member.getMemberId(), dto.getFcmToken());
			if (result == 0) {
				throw new RuntimeException("FCM 토큰 업데이트 실패: 사용자를 찾을 수 없습니다.");
			}
			log.info("FCM 토큰 업데이트 성공. member_id: {}", member.getMemberId());
		} catch (Exception e) {
			log.error("FCM 토큰 업데이트 실패. member_id: {}", member.getMemberId(), e);
			throw e;
		}
		return true;
	}

	@Override
	@Transactional
	public boolean deleteFcmToken(){
		// 1. 현재 로그인된 사용자 정보 조회
		UserDetails userDetails = null;
		try {
			Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			if (principal instanceof UserDetails) {
				userDetails = (UserDetails) principal;
			} else {
				log.warn("인증된 사용자 정보가 UserDetails 타입이 아닙니다.");
				throw new BusinessException(StatusCode.UNAUTHORIZED);
			}
		} catch (Exception e) {
			log.error("인증된 사용자 정보를 가져오는 데 실패했습니다.", e);
			throw new BusinessException(StatusCode.UNAUTHORIZED);
		}

		// 2. Member 정보 조회
		Member member;
		try {
			member = memberMapper.getByEmail(userDetails.getUsername());
			if (member == null) {
				log.warn("사용자 이메일({})에 해당하는 회원 정보를 찾을 수 없습니다.", userDetails.getUsername());
				throw new BusinessException(StatusCode.BAD_REQUEST, "해당하는 사용자 이메일의 유저가 없습니다.");
			}
		} catch (DataAccessException e) {
			log.error("회원 정보 조회 중 데이터베이스 오류 발생", e);
			throw new BusinessException(StatusCode.INTERNAL_ERROR, "회원 정보 조회 중 오류가 발생했습니다.");
		}
		log.info("member : {} ",member);

		try{
			int result = memberMapper.deleteFcmToken(member.getMemberId());
			if (result == 0) {
				throw new RuntimeException("FCM 토큰 업데이트 실패: 사용자를 찾을 수 없습니다.");
			}
			log.info("FCM 토큰 업데이트 성공. member_id: {}", member.getMemberId());
		} catch (Exception e) {
			log.error("FCM 토큰 업데이트 실패. member_id: {}", member.getMemberId(), e);
			throw e;
		}
		return true;
	}

	@Transactional
	@Override
	public Member getByMemberId(Long memberId) {
		return memberMapper.getByMemberId(memberId);
	}

	@Transactional
	@Override
	public boolean verifyJoin(MemberVerifyRequestDto dto) {
		if (!verifyAccountNumber(dto.getName(), dto.getAccountNumber(), dto.getAccountPassword())) {
			throw new IllegalArgumentException("일치하는 계좌 정보가 없습니다.");
		}

		if (!validateNameAndIdCardNumber(dto.getName(), dto.getIdCardNumber())) {
			throw new IllegalArgumentException("일치하는 신분증 정보가 없습니다.");
		}

		return true;
	}

	@Transactional
	@Override
	public boolean userJoin(MemberJoinRequestDto dto) {

		//이름과 주민등록번호에 맞는 사람이 있는지 확인
		if (!validateNameAndIdCardNumber(dto.getName(), dto.getIdCardNumber())) {
			throw new IllegalArgumentException("일치하는 신분증 정보가 없습니다.");
		}

		//계좌번호와 계좌비밀번호에 맞는 사람이 있는지 확인
		if (!validateAccountNumber(dto.getAccountNumber(), dto.getAccountPassword())) {
			throw new IllegalArgumentException("일치하는 계좌 정보가 없습니다.");
		}

		// // Member 생성 및 저장
		// Member member = Member.builder()
		// 	.memberType(dto.getRole())
		// 	.email(dto.getEmail())
		// 	.password(hashUtil.hash(dto.getPassword()))
		// 	.name(dto.getName())
		// 	.idCardNumber(hashUtil.hash(dto.getIdCardNumber()))
		// 	.createdAt(java.time.LocalDateTime.now())
		// 	.updatedAt(java.time.LocalDateTime.now())
		// 	.build();

		// Member 생성 및 저장 -> 비밀번호 평문으로
		Member member = Member.builder()
			.memberType(dto.getRole())
			.email(dto.getEmail())
			.password(dto.getPassword())
			.name(dto.getName())
			.idCardNumber(dto.getIdCardNumber())
			.createdAt(java.time.LocalDateTime.now())
			.updatedAt(java.time.LocalDateTime.now())
			.build();

		//새로운 멤버 생성후 DB에 저장
		memberMapper.insert(member);
		Long memberId = member.getMemberId();

		//주민등록증 - 멤버 매핑
		idCardMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		//운전면허증 - 멤버 매핑
		driverLicenseMapper.updateMemberIdByIdCardNumber(dto.getIdCardNumber(), memberId);

		//계좌 - 멤버 매핑
		accountMapper.updateMemberIdByAccountNumber(dto.getAccountNumber(), memberId);

		return true;

	}

	public boolean validateAccountNumber(String accountNumber, String accountPassword) {
		return accountMapper.existsByAccountNumberAndAccountPassword(accountNumber, accountPassword);
	}

	public boolean verifyAccountNumber(String name, String accountNumber, String accountPassword) {
		return accountMapper.existsByNameAndAccountNumberAndAccountPassword(name, accountNumber, accountPassword);
	}

	public boolean validateNameAndIdCardNumber(String name, String idCardNumber) {
		return idCardMapper.existsByNameAndIdCardNumber(name, idCardNumber);
	}

	@Override
	public Long searchUserIdByEmail(String email, Long tripId) {
		Long memberId = memberMapper.getByEmail(email).getMemberId();
		int count = tripMemberMapper.existMemberInTrip(tripId,memberId);
		if(count > 0){
			throw new BusinessException(StatusCode.BAD_REQUEST,"이미 유저가 여행에 존재합니다");
		}
		return memberId;
	}

	@Override
	public List<TripMemberResponseDto> getTripMembers(Long tripId){
		// 1. tripId를 기반으로 TRIP_MEMBER 에서 member_id 들 뽑기
		List<TripMember> tripMembers = tripMemberMapper.searchTripMembersByTripId(tripId);

		List<TripMemberResponseDto> dtos = new ArrayList<>();

		for(TripMember m : tripMembers){
			Member member = memberMapper.getByMemberId(m.getMemberId());
			// 2. member_id 를 기반으로 MEMBER 에서 member_name 뽑기
			dtos.add(TripMemberResponseDto.builder()
							.memberId(member.getMemberId())
							.memberName(member.getName())
							.memberEmail(member.getEmail())
							.build());
		}
		return dtos;
	}
}
