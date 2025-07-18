package org.moa.global.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.global.test.domain.User;

@Mapper
public interface UserMapper {
	User findByName(String name);

	void insertUser(User user);
}
