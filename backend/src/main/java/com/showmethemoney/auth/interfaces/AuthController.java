package com.showmethemoney.auth.interfaces;

import com.showmethemoney.auth.application.AuthService;
import com.showmethemoney.auth.interfaces.dto.LoginRequest;
import com.showmethemoney.auth.interfaces.dto.LoginResponse;
import com.showmethemoney.auth.interfaces.dto.SignupRequest;
import com.showmethemoney.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // AccessToken only — 클라이언트에서 토큰 삭제로 처리
        return ApiResponse.ok();
    }
}
