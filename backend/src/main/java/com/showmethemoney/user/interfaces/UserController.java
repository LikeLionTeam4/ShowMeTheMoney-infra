package com.showmethemoney.user.interfaces;

import com.showmethemoney.common.ApiResponse;
import com.showmethemoney.user.application.UserService;
import com.showmethemoney.user.interfaces.dto.UpdateUserRequest;
import com.showmethemoney.user.interfaces.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getMe(userId));
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateMe(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody UpdateUserRequest request) {
        userService.updateMe(userId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(@AuthenticationPrincipal Long userId) {
        userService.deleteMe(userId);
        return ApiResponse.ok();
    }
}
