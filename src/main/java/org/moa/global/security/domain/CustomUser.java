package org.moa.global.security.domain;

import java.util.Collection;
import java.util.Collections;

import org.moa.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomUser extends User {
	private Member member;

	public CustomUser(String email, String password, Collection<? extends GrantedAuthority> authorities) {
		super(email, password, authorities);
	}

	public CustomUser(Member member) {
		super(
			member.getEmail(),
			member.getPassword(),
			Collections.singletonList(new SimpleGrantedAuthority(member.getMemberType().name()))
		);
		this.member = member;
	}
}
