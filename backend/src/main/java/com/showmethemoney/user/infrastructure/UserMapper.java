package com.showmethemoney.user.infrastructure;

import com.showmethemoney.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User findById(Long id);

    void updateNickname(Long id, String nickname);

    void softDelete(Long id);
}
