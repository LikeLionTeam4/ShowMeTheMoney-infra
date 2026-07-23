package com.showmethemoney.transaction.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.config.SecurityConfig;
import com.showmethemoney.transaction.application.TransactionService;
import com.showmethemoney.transaction.interfaces.dto.CreateTransactionRequest;
import com.showmethemoney.transaction.interfaces.dto.TransactionPageResponse;
import com.showmethemoney.transaction.interfaces.dto.TransactionResponse;
import com.showmethemoney.transaction.interfaces.dto.UpdateTransactionRequest;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean TransactionService transactionService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static TransactionResponse sampleResponse() {
        return new TransactionResponse(1L, "0", "001", "식비",
                new BigDecimal("15000"), "점심", LocalDate.of(2026, 6, 29));
    }

    private static TransactionPageResponse samplePageResponse() {
        return new TransactionPageResponse(List.of(sampleResponse()), 1, 1,
                BigDecimal.ZERO, new BigDecimal("15000"));
    }

    @Test
    void 내역_생성() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                0, "001", new BigDecimal("15000"), "점심", LocalDate.of(2026, 6, 29));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 내역_생성_유효성검사_실패() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                null, "", new BigDecimal("-1000"), null, null);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 내역_목록조회() throws Exception {
        given(transactionService.getList(eq(1L), any())).willReturn(samplePageResponse());

        mockMvc.perform(get("/api/transactions").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].categoryCode").value("001"));
    }

    @Test
    void 내역_단건조회() throws Exception {
        given(transactionService.getOne(1L, 1L)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/transactions/1").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.memo").value("점심"));
    }

    @Test
    void 내역_수정() throws Exception {
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, null, new BigDecimal("20000"), "저녁", null);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 내역_삭제() throws Exception {
        mockMvc.perform(delete("/api/transactions/1").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 인증없이_401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 목록조회_type_파라미터_타입오류_400() throws Exception {
        mockMvc.perform(get("/api/transactions").param("type", "abc").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 단건조회_숫자아닌_id_400() throws Exception {
        mockMvc.perform(get("/api/transactions/abc").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
