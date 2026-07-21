package com.showmethemoney.user.application;

import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import com.showmethemoney.user.domain.User;
import com.showmethemoney.user.infrastructure.UserMapper;
import com.showmethemoney.user.interfaces.dto.UpdateUserRequest;
import com.showmethemoney.user.interfaces.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findActiveUser(userId);
        return new UserResponse(user.getUuid(), user.getUsername(), user.getEmail(), user.getNickname(), "user");
    }

    @Transactional
    public void updateMe(Long userId, UpdateUserRequest request) {
        findActiveUser(userId);
        userMapper.updateNickname(userId, request.name());
    }

    @Transactional
    public void deleteMe(Long userId) {
        findActiveUser(userId);
        userMapper.softDelete(userId);
    }

    private User findActiveUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
