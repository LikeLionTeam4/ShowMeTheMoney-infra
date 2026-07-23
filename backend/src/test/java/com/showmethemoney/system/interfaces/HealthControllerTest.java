package com.showmethemoney.system.interfaces;

import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HealthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean JdbcTemplate jdbcTemplate;

    @Test
    void 헬스체크_DB정상() throws Exception {
        given(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).willReturn(1);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.db").value("connected"));
    }

    @Test
    void 헬스체크_DB오류() throws Exception {
        given(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class)))
                .willThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.db").value("disconnected"));
    }

    @Test
    void 헬스체크_인증없이_접근가능() throws Exception {
        given(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).willReturn(1);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void 핑_DB오류여도_정상응답() throws Exception {
        given(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class)))
                .willThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
