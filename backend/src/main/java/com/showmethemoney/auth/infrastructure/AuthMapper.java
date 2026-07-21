package com.showmethemoney.auth.infrastructure;

import com.showmethemoney.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {

    void insertUser(User user);

    User findByUsername(String username);

    User findByEmail(String email);
}
