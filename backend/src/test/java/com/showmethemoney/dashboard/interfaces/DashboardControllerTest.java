package com.showmethemoney.dashboard.interfaces;

import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.config.SecurityConfig;
import com.showmethemoney.dashboard.application.DashboardService;
import com.showmethemoney.dashboard.interfaces.dto.CategoryExpenseResponse;
import com.showmethemoney.dashboard.interfaces.dto.DashboardSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean DashboardService dashboardService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void 대시보드_요약_조회() throws Exception {
        given(dashboardService.getSummary(1L, "202606")).willReturn(
                new DashboardSummaryResponse("2026-06",
                        new BigDecimal("3000000"), new BigDecimal("1500000"),
                        new BigDecimal("1500000"), new BigDecimal("2000000"),
                        75.0, false));

        mockMvc.perform(get("/api/dashboard/summary").param("yearMonth", "202606")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-06"))
                .andExpect(jsonPath("$.data.isOverBudget").value(false));
    }

    @Test
    void 대시보드_카테고리별_지출_조회() throws Exception {
        given(dashboardService.getCategoryExpenses(1L, "202606", null)).willReturn(List.of(
                new CategoryExpenseResponse("001", "식비", new BigDecimal("300000"), 20.0),
                new CategoryExpenseResponse("003", "교통", new BigDecimal("150000"), 10.0)
        ));

        mockMvc.perform(get("/api/dashboard/categories").param("yearMonth", "202606")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].categoryCode").value("001"));
    }

    @Test
    void 대시보드_카테고리별_타입_필터() throws Exception {
        given(dashboardService.getCategoryExpenses(1L, "202606", 0)).willReturn(List.of(
                new CategoryExpenseResponse("001", "식비", new BigDecimal("300000"), 20.0)
        ));

        mockMvc.perform(get("/api/dashboard/categories")
                        .param("yearMonth", "202606").param("type", "0")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void 인증없이_401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("yearMonth", "202606"))
                .andExpect(status().isUnauthorized());
    }
}
