package com.showmethemoney.budget.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.budget.application.BudgetService;
import com.showmethemoney.budget.interfaces.dto.BudgetResponse;
import com.showmethemoney.budget.interfaces.dto.CreateBudgetRequest;
import com.showmethemoney.budget.interfaces.dto.UpdateBudgetRequest;
import com.showmethemoney.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BudgetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean BudgetService budgetService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void 예산_생성() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest("202606", new BigDecimal("500000"), null);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 예산_생성_유효성검사_실패() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest("", new BigDecimal("-1000"), null);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 예산_조회() throws Exception {
        given(budgetService.get(1L, "202606"))
                .willReturn(new BudgetResponse(1L, "2026-06", new BigDecimal("500000")));

        mockMvc.perform(get("/api/budgets").param("yearMonth", "202606").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-06"))
                .andExpect(jsonPath("$.data.amount").value(500000));
    }

    @Test
    void 예산_수정() throws Exception {
        UpdateBudgetRequest request = new UpdateBudgetRequest(new BigDecimal("600000"), null);

        mockMvc.perform(put("/api/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 인증없이_401() throws Exception {
        mockMvc.perform(get("/api/budgets").param("yearMonth", "202606"))
                .andExpect(status().isUnauthorized());
    }
}
