package com.showmethemoney.auth.application;

import com.showmethemoney.auth.infrastructure.AuthMapper;
import com.showmethemoney.auth.interfaces.dto.LoginRequest;
import com.showmethemoney.auth.interfaces.dto.LoginResponse;
import com.showmethemoney.auth.interfaces.dto.SignupRequest;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.user.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (authMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (authMapper.findByEmail(request.email()) != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.of(request.username(), request.email(), passwordEncoder.encode(request.password()), request.name());
        authMapper.insertUser(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = authMapper.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtTokenProvider.generateToken(user.getUuid(), user.getEmail(), "user");
        return new LoginResponse(token, "Bearer", 3600L);
    }
}
