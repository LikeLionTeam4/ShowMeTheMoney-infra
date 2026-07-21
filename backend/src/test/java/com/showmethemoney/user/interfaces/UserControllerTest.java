package com.showmethemoney.user.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.config.SecurityConfig;
import com.showmethemoney.user.application.UserService;
import com.showmethemoney.user.interfaces.dto.UpdateUserRequest;
import com.showmethemoney.user.interfaces.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserService userService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void 내정보_조회() throws Exception {
        given(userService.getMe(1L)).willReturn(new UserResponse(1L, "testuser", "test@example.com", "테스트", "user"));

        mockMvc.perform(get("/api/users/me").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("테스트"));
    }

    @Test
    void 내정보_수정() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("새이름");

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 내정보_수정_유효성검사_실패() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("");

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 회원탈퇴() throws Exception {
        mockMvc.perform(delete("/api/users/me").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 인증없이_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
