package org.moa.global.security.service;

import org.moa.global.security.domain.CustomUser;
import org.moa.member.entity.Member;
import org.moa.member.mapper.MemberMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	private final MemberMapper memberMapper;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Member member = memberMapper.getByEmail(email);
		if (member == null) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
		}
		return new CustomUser(member);
	}

	public UserDetails loadUserByMemberId(Long memberId) {
		Member member = memberMapper.getByMemberId(memberId);
		if (member == null) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다. " + memberId);
		}
		return new CustomUser(member);
	}
}
