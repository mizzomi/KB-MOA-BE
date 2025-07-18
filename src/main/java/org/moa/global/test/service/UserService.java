package org.moa.global.test.service;

import org.moa.global.test.domain.User;
import org.moa.global.test.domain.UserRequestDto;
import org.moa.global.test.domain.UserResponseDto;
import org.moa.global.test.mapper.UserMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;

	public UserResponseDto getUser(String name) {
		User user = userMapper.findByName(name);
		return UserResponseDto.of(user);
	}

	public UserResponseDto createUser(UserRequestDto dto) {
		User user = dto.toEntity();
		userMapper.insertUser(user);

		return UserResponseDto.of(user);
	}

}
